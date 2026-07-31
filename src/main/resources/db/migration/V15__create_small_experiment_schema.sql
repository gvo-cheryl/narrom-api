-- 작은 실험(Small Experiment) 도메인: docs/instruction/experiment/Naroom_Beta1_작은_실험_기획_설계.md 기준.
-- docs/instruction/experiment/V4__create_small_experiment_schema.sql 초안을 이 저장소의 실제 마이그레이션
-- 번호(V15)로 옮기면서, 저장소 전반의 관례에 맞춰 varchar+CHECK로 표현된 닫힌 값 집합을 네이티브
-- Postgres ENUM 타입으로 바꿨다(ai_job_status/entry_type/summary_scope 등 기존 패턴과 동일).
-- duration_days처럼 숫자 범위인 값과 response_type처럼 아직 확정되지 않은 개방형 문자열은 그대로 둔다.

CREATE TYPE "experiment_mission_type" AS ENUM (
    'OBSERVATION', 'QUESTION', 'ACTION', 'RECORD', 'REVIEW'
);

CREATE TYPE "experiment_emotional_load" AS ENUM (
    'LOW', 'MEDIUM', 'HIGH'
);

-- experiment_programs.source_type과 user_experiment_programs.configuration_source가 공유한다 -
-- 코스 원본이 어떻게 만들어졌는지와 사용자가 실제로 어떤 경로로 시작했는지는 같은 값 집합이다.
CREATE TYPE "experiment_source_type" AS ENUM (
    'TEMPLATE', 'RANDOM', 'AI_RECOMMENDED', 'USER_COMPOSED'
);

CREATE TYPE "experiment_program_status" AS ENUM (
    'DRAFT', 'PUBLISHED', 'ARCHIVED'
);

CREATE TYPE "user_experiment_program_status" AS ENUM (
    'READY', 'IN_PROGRESS', 'PAUSED', 'AWAITING_REVIEW', 'COMPLETED', 'ENDED_EARLY'
);

CREATE TYPE "user_program_mission_slot_status" AS ENUM (
    'PENDING', 'CURRENT', 'RECORDED'
);

CREATE TYPE "mission_replacement_reason_code" AS ENUM (
    'TOO_HEAVY', 'NOT_RELEVANT', 'NOT_A_FIT', 'WANT_LIGHTER', 'WANT_DIFFERENT_TYPE', 'RANDOM', 'OTHER'
);

CREATE TYPE "experiment_attempt_status" AS ENUM (
    'DONE', 'PARTIALLY_DONE', 'RESTED', 'TRIED_DIFFERENTLY', 'NOT_A_FIT', 'RECORD_ONLY'
);

CREATE TYPE "experiment_recommendation_source_type" AS ENUM (
    'BROWSE', 'RECORD', 'CHECK_IN', 'WEEKLY_REFLECTION', 'RULE', 'RANDOM', 'AI'
);

CREATE TYPE "experiment_recommendation_status" AS ENUM (
    'SHOWN', 'VIEWED', 'ACCEPTED', 'DISMISSED', 'EXPIRED'
);

CREATE TABLE "experiment_topics" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "code" varchar(50) NOT NULL UNIQUE,
    "name" varchar(80) NOT NULL,
    "description" text NULL,
    "display_order" integer NOT NULL DEFAULT 0,
    "active" boolean NOT NULL DEFAULT true,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "ck_experiment_topics_code_not_blank" CHECK (BTRIM("code") <> ''),
    CONSTRAINT "ck_experiment_topics_name_not_blank" CHECK (BTRIM("name") <> ''),
    CONSTRAINT "ck_experiment_topics_display_order" CHECK ("display_order" >= 0)
);

CREATE TABLE "experiment_missions" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "code" varchar(80) NOT NULL UNIQUE,
    "content_version" integer NOT NULL DEFAULT 1 CHECK ("content_version" > 0),
    "topic_id" uuid NOT NULL REFERENCES "experiment_topics"("id"),
    "title" varchar(120) NOT NULL,
    "description" text NOT NULL,
    "instruction" text NOT NULL,
    "mission_type" "experiment_mission_type" NOT NULL,
    "response_type" varchar(40) NOT NULL,
    "estimated_minutes" smallint NOT NULL CHECK ("estimated_minutes" BETWEEN 1 AND 30),
    "emotional_load" "experiment_emotional_load" NOT NULL DEFAULT 'LOW',
    "reflection_questions" jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (jsonb_typeof("reflection_questions") = 'array'),
    "examples" jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (jsonb_typeof("examples") = 'array'),
    "response_schema" jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof("response_schema") = 'object'),
    "safety_note" text NULL,
    "active" boolean NOT NULL DEFAULT true,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE "experiment_programs" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "code" varchar(80) NOT NULL UNIQUE,
    "content_version" integer NOT NULL DEFAULT 1 CHECK ("content_version" > 0),
    "primary_topic_id" uuid NOT NULL REFERENCES "experiment_topics"("id"),
    "title" varchar(120) NOT NULL,
    "description" text NOT NULL,
    "duration_days" smallint NOT NULL CHECK ("duration_days" IN (3, 7, 14, 28)),
    "source_type" "experiment_source_type" NOT NULL DEFAULT 'TEMPLATE',
    "status" "experiment_program_status" NOT NULL DEFAULT 'DRAFT',
    "estimated_minutes_min" smallint NOT NULL CHECK ("estimated_minutes_min" BETWEEN 1 AND 30),
    "estimated_minutes_max" smallint NOT NULL CHECK ("estimated_minutes_max" BETWEEN 1 AND 30),
    "is_featured" boolean NOT NULL DEFAULT false,
    "is_beginner" boolean NOT NULL DEFAULT false,
    "display_order" integer NOT NULL DEFAULT 0,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "ck_experiment_programs_minutes_range" CHECK ("estimated_minutes_min" <= "estimated_minutes_max")
);

CREATE TABLE "experiment_program_missions" (
    "program_id" uuid NOT NULL REFERENCES "experiment_programs"("id") ON DELETE CASCADE,
    "mission_id" uuid NOT NULL REFERENCES "experiment_missions"("id"),
    "day_number" smallint NOT NULL CHECK ("day_number" > 0),
    "is_replaceable" boolean NOT NULL DEFAULT true,
    "replacement_group" varchar(50) NULL,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY ("program_id", "day_number"),
    UNIQUE ("program_id", "mission_id")
);

CREATE TABLE "user_experiment_programs" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members"("id") ON DELETE CASCADE,
    "program_id" uuid NULL REFERENCES "experiment_programs"("id"),
    "program_version" integer NULL CHECK ("program_version" IS NULL OR "program_version" > 0),
    "configuration_source" "experiment_source_type" NOT NULL,
    "status" "user_experiment_program_status" NOT NULL DEFAULT 'READY',
    "title_snapshot" varchar(120) NOT NULL,
    "description_snapshot" text NOT NULL,
    "duration_days" smallint NOT NULL CHECK ("duration_days" IN (3, 7, 14, 28)),
    "current_day" smallint NOT NULL DEFAULT 1 CHECK ("current_day" > 0),
    "started_at" timestamptz NULL,
    "target_end_date" date NULL,
    "paused_at" timestamptz NULL,
    "review_ready_at" timestamptz NULL,
    "completed_at" timestamptz NULL,
    "ended_early_at" timestamptz NULL,
    "last_activity_date" date NULL,
    "review_data" jsonb NULL CHECK ("review_data" IS NULL OR jsonb_typeof("review_data") = 'object'),
    "user_summary" text NULL,
    "review_entry_id" uuid NULL REFERENCES "entries"("id") ON DELETE SET NULL,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    "version" bigint NOT NULL DEFAULT 0,
    CONSTRAINT "ck_user_experiment_programs_current_day" CHECK ("current_day" <= "duration_days"),
    CONSTRAINT "ck_user_experiment_programs_completed_at" CHECK (("status" <> 'COMPLETED') OR "completed_at" IS NOT NULL),
    CONSTRAINT "ck_user_experiment_programs_ended_early_at" CHECK (("status" <> 'ENDED_EARLY') OR "ended_early_at" IS NOT NULL)
);

-- DEC-02: 활성 상태(IN_PROGRESS/PAUSED/AWAITING_REVIEW)는 회원당 1개만 허용한다.
CREATE UNIQUE INDEX "uq_user_experiment_one_active"
    ON "user_experiment_programs"("member_id")
    WHERE "status" IN ('IN_PROGRESS', 'PAUSED', 'AWAITING_REVIEW');

CREATE INDEX "ix_user_experiment_programs_member_history"
    ON "user_experiment_programs"("member_id", "created_at" DESC);

CREATE TABLE "user_program_missions" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "user_experiment_program_id" uuid NOT NULL
        REFERENCES "user_experiment_programs"("id") ON DELETE CASCADE,
    "day_number" smallint NOT NULL CHECK ("day_number" > 0),
    "mission_id" uuid NULL REFERENCES "experiment_missions"("id") ON DELETE SET NULL,
    "original_mission_id" uuid NULL REFERENCES "experiment_missions"("id") ON DELETE SET NULL,
    "mission_version" integer NOT NULL CHECK ("mission_version" > 0),
    "title_snapshot" varchar(120) NOT NULL,
    "description_snapshot" text NOT NULL,
    "instruction_snapshot" text NOT NULL,
    "mission_type" "experiment_mission_type" NOT NULL,
    "response_type" varchar(40) NOT NULL,
    "estimated_minutes" smallint NOT NULL CHECK ("estimated_minutes" BETWEEN 1 AND 30),
    "reflection_questions_snapshot" jsonb NOT NULL DEFAULT '[]'::jsonb
        CHECK (jsonb_typeof("reflection_questions_snapshot") = 'array'),
    "response_schema_snapshot" jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof("response_schema_snapshot") = 'object'),
    "slot_status" "user_program_mission_slot_status" NOT NULL DEFAULT 'PENDING',
    "replacement_count" integer NOT NULL DEFAULT 0 CHECK ("replacement_count" >= 0),
    "first_available_at" timestamptz NULL,
    "recorded_at" timestamptz NULL,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("user_experiment_program_id", "day_number"),
    -- experiment_mission_records가 (user_program_mission_id, user_experiment_program_id) 복합 FK로
    -- 참조하기 위한 대상 - 슬롯이 반드시 자신이 속한 코스를 통해서만 조회되도록 강제한다.
    UNIQUE ("id", "user_experiment_program_id")
);

CREATE INDEX "ix_user_program_missions_current"
    ON "user_program_missions"("user_experiment_program_id", "slot_status", "day_number");

CREATE TABLE "user_program_mission_replacements" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "user_program_mission_id" uuid NOT NULL
        REFERENCES "user_program_missions"("id") ON DELETE CASCADE,
    "from_mission_id" uuid NULL REFERENCES "experiment_missions"("id") ON DELETE SET NULL,
    "to_mission_id" uuid NULL REFERENCES "experiment_missions"("id") ON DELETE SET NULL,
    "reason_code" "mission_replacement_reason_code" NULL,
    "reason_note" text NULL,
    "replaced_at" timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE "experiment_mission_records" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "user_experiment_program_id" uuid NOT NULL
        REFERENCES "user_experiment_programs"("id") ON DELETE CASCADE,
    "user_program_mission_id" uuid NOT NULL,
    "record_date" date NOT NULL,
    "attempt_status" "experiment_attempt_status" NOT NULL,
    "response_text" text NULL,
    "response_data" jsonb NULL CHECK ("response_data" IS NULL OR jsonb_typeof("response_data") = 'object'),
    "emotion_data" jsonb NULL CHECK ("emotion_data" IS NULL OR jsonb_typeof("emotion_data") = 'object'),
    "energy_level" smallint NULL CHECK ("energy_level" BETWEEN 1 AND 5),
    "reflection" text NULL,
    "entry_id" uuid NULL REFERENCES "entries"("id") ON DELETE SET NULL,
    "recorded_at" timestamptz NOT NULL DEFAULT now(),
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY ("user_program_mission_id", "user_experiment_program_id")
        REFERENCES "user_program_missions"("id", "user_experiment_program_id")
        ON DELETE CASCADE
);

-- RESTED는 슬롯을 소비하지 않으므로 소비 여부를 따지는 유일성 제약에서 제외한다.
CREATE UNIQUE INDEX "uq_experiment_mission_consumed_once"
    ON "experiment_mission_records"("user_program_mission_id")
    WHERE "attempt_status" <> 'RESTED';

CREATE UNIQUE INDEX "uq_experiment_one_rest_per_date"
    ON "experiment_mission_records"("user_experiment_program_id", "record_date")
    WHERE "attempt_status" = 'RESTED';

CREATE INDEX "ix_experiment_mission_records_member_timeline"
    ON "experiment_mission_records"("user_experiment_program_id", "record_date" DESC);

CREATE TABLE "experiment_recommendations" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members"("id") ON DELETE CASCADE,
    "program_id" uuid NOT NULL REFERENCES "experiment_programs"("id"),
    "source_type" "experiment_recommendation_source_type" NOT NULL,
    "source_entry_id" uuid NULL REFERENCES "entries"("id") ON DELETE SET NULL,
    "source_period_reflection_id" uuid NULL REFERENCES "period_reflections"("id") ON DELETE SET NULL,
    "reason_code" varchar(80) NULL,
    "reason_text" text NULL,
    "evidence" jsonb NULL CHECK ("evidence" IS NULL OR jsonb_typeof("evidence") = 'object'),
    "status" "experiment_recommendation_status" NOT NULL DEFAULT 'SHOWN',
    "started_user_program_id" uuid NULL
        REFERENCES "user_experiment_programs"("id") ON DELETE SET NULL,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "viewed_at" timestamptz NULL,
    "responded_at" timestamptz NULL,
    "expires_at" timestamptz NULL
);

CREATE INDEX "ix_experiment_recommendations_member_status"
    ON "experiment_recommendations"("member_id", "status", "created_at" DESC);

-- 코스 종료 시점에 요청한 3일 코스 전용 AI 회고를 이 작은 실험과 연결해 LifeTime에서 추적할 수 있게 한다.
ALTER TABLE "period_reflections"
    ADD COLUMN "user_experiment_program_id" uuid NULL
        REFERENCES "user_experiment_programs"("id") ON DELETE SET NULL;

CREATE INDEX "ix_period_reflections_user_experiment"
    ON "period_reflections"("user_experiment_program_id")
    WHERE "user_experiment_program_id" IS NOT NULL;
