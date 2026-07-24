-- Naroom Beta 1
-- V2에서 만든 Record 4개 테이블(tags, entries, entry_tags, entry_self_reflections)을
-- reference 스키마(naroom_beta1_full_schema_reference.sql) 기준으로 재생성한다.
-- V2는 이미 적용된 마이그레이션이라 직접 수정하지 않는다.
--
-- Check-in 2개 테이블(check_ins, check_in_emotions)은 entries/tags를 참조하고 있어
-- 먼저 DROP한다. 아직 비어 있고 Check-in 도메인 작업 시 reference 기준으로 다시 만든다.
--
-- entry_self_reflections.ai_reflection_id는 reference상 ai_reflections를 참조하지만
-- AI 도메인이 아직 없어 지금은 FK 없는 컬럼으로만 둔다. AI 도메인 구현 시 FK를 추가한다.
--
-- pg_trgm은 reference 스키마 맨 앞에서 요구하지만 V1에는 pgcrypto만 설치되고 빠져 있었다.
-- V1은 이미 적용된 마이그레이션이라 여기서 대신 설치한다.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

DROP TABLE "check_in_emotions";
DROP TABLE "check_ins";
DROP TABLE "entry_tags";
DROP TABLE "entry_self_reflections";
DROP TABLE "entries";
DROP TABLE "tags";

DROP TYPE "tag_scope";
DROP TYPE "entry_tag_source";
DROP TYPE "entry_tag_status";

CREATE TYPE "tag_scope" AS ENUM ('SYSTEM', 'USER');
CREATE TYPE "tag_source" AS ENUM ('USER', 'AI', 'CHECK_IN', 'REFLECTION', 'EXPERIMENT');
CREATE TYPE "tag_state" AS ENUM ('SUGGESTED', 'CONFIRMED', 'REJECTED', 'SYSTEM');

-- ---------------------------------------------------------------------------
-- Record
-- ---------------------------------------------------------------------------

CREATE TABLE "tags" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "owner_member_id" uuid REFERENCES "members" ("id") ON DELETE CASCADE,
    "scope" tag_scope NOT NULL,
    "category" tag_category NOT NULL,
    "name" varchar(80) NOT NULL,
    "normalized_name" varchar(80) NOT NULL,
    "active" boolean NOT NULL DEFAULT true,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "ck_tags_1" CHECK ((scope = 'SYSTEM' AND owner_member_id IS NULL) OR (scope = 'USER' AND owner_member_id IS NOT NULL))
);
COMMENT ON TABLE "tags" IS '시스템 및 사용자 정의 감정·상황·욕구·가치·행동·회복 태그 사전';
COMMENT ON COLUMN "tags"."id" IS '태그 ID';
COMMENT ON COLUMN "tags"."owner_member_id" IS 'USER 범위 태그 소유 회원';
COMMENT ON COLUMN "tags"."scope" IS '시스템/회원 전용 범위';
COMMENT ON COLUMN "tags"."category" IS '태그 분류';
COMMENT ON COLUMN "tags"."name" IS '사용자 표시 이름';
COMMENT ON COLUMN "tags"."normalized_name" IS '중복 확인용 정규화 이름';
COMMENT ON COLUMN "tags"."active" IS '사용 가능 여부';
COMMENT ON COLUMN "tags"."created_at" IS '생성 시각';
COMMENT ON COLUMN "tags"."updated_at" IS '수정 시각';
CREATE UNIQUE INDEX "uq_tags_system_name" ON "tags" ("category", "normalized_name") WHERE scope = 'SYSTEM';
CREATE UNIQUE INDEX "uq_tags_user_name" ON "tags" ("owner_member_id", "category", "normalized_name") WHERE scope = 'USER';
CREATE INDEX "ix_tags_search_name" ON "tags" USING gin ("normalized_name" gin_trgm_ops);
CREATE TRIGGER "trg_tags_updated_at" BEFORE UPDATE ON "tags" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "entries" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "entry_type" entry_type NOT NULL,
    "status" entry_status NOT NULL DEFAULT 'DRAFT',
    "title" varchar(200),
    "body" text,
    "record_date" date NOT NULL,
    "parent_entry_id" uuid REFERENCES "entries" ("id") ON DELETE SET NULL,
    "quote_id" uuid REFERENCES "quotes" ("id") ON DELETE SET NULL,
    "prompt_snapshot" text,
    "ai_processing_allowed" boolean NOT NULL DEFAULT true,
    "published_at" timestamptz,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    "version" bigint NOT NULL DEFAULT 0
);
COMMENT ON TABLE "entries" IS 'LifeTime의 공통 기록 봉투; 구조화 기록도 하나의 타임라인으로 통합';
COMMENT ON COLUMN "entries"."id" IS '기록 ID';
COMMENT ON COLUMN "entries"."member_id" IS '기록 소유 회원';
COMMENT ON COLUMN "entries"."entry_type" IS '기록 유형';
COMMENT ON COLUMN "entries"."status" IS '임시저장/발행 상태';
COMMENT ON COLUMN "entries"."title" IS '선택 제목';
COMMENT ON COLUMN "entries"."body" IS '기록 원문; 구조화 기록은 비어 있을 수 있음';
COMMENT ON COLUMN "entries"."record_date" IS '사용자 시간대 기준 기록 날짜';
COMMENT ON COLUMN "entries"."parent_entry_id" IS '체크인에서 이어 쓴 기록 등 원본 기록';
COMMENT ON COLUMN "entries"."quote_id" IS '문장 기반 기록의 원문';
COMMENT ON COLUMN "entries"."prompt_snapshot" IS '질문형 기록 당시 화면에 보인 질문';
COMMENT ON COLUMN "entries"."ai_processing_allowed" IS '이 기록의 AI 처리 허용 여부';
COMMENT ON COLUMN "entries"."published_at" IS '기록 완료 시각';
COMMENT ON COLUMN "entries"."created_at" IS '생성 시각';
COMMENT ON COLUMN "entries"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "entries"."version" IS 'JPA 낙관적 잠금 버전';
CREATE INDEX "ix_entries_member_timeline" ON "entries" ("member_id", "record_date", "created_at");
CREATE INDEX "ix_entries_member_type" ON "entries" ("member_id", "entry_type", "record_date");
CREATE INDEX "ix_entries_body_trgm" ON "entries" USING gin ("body" gin_trgm_ops) WHERE body IS NOT NULL;
CREATE TRIGGER "trg_entries_updated_at" BEFORE UPDATE ON "entries" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "entry_tags" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
    "tag_id" uuid NOT NULL REFERENCES "tags" ("id") ON DELETE RESTRICT,
    "source" tag_source NOT NULL,
    "state" tag_state NOT NULL,
    "confidence" numeric(5, 4),
    "evidence_excerpt" text,
    "evidence_start" integer,
    "evidence_end" integer,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("entry_id", "tag_id"),
    CONSTRAINT "ck_entry_tags_1" CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT "ck_entry_tags_2" CHECK ((evidence_start IS NULL AND evidence_end IS NULL) OR (evidence_start >= 0 AND evidence_end > evidence_start))
);
COMMENT ON TABLE "entry_tags" IS '기록과 태그의 연결 및 AI 제안에 대한 사용자 통제 상태';
COMMENT ON COLUMN "entry_tags"."id" IS '기록-태그 연결 ID';
COMMENT ON COLUMN "entry_tags"."entry_id" IS '대상 기록';
COMMENT ON COLUMN "entry_tags"."tag_id" IS '연결 태그';
COMMENT ON COLUMN "entry_tags"."source" IS '생성 출처';
COMMENT ON COLUMN "entry_tags"."state" IS '제안/확인/제외/시스템 상태';
COMMENT ON COLUMN "entry_tags"."confidence" IS 'AI 제안 신뢰도 0~1';
COMMENT ON COLUMN "entry_tags"."evidence_excerpt" IS '근거가 된 기록 구간의 짧은 사본';
COMMENT ON COLUMN "entry_tags"."evidence_start" IS '원문 내 시작 문자 위치';
COMMENT ON COLUMN "entry_tags"."evidence_end" IS '원문 내 끝 문자 위치';
COMMENT ON COLUMN "entry_tags"."created_at" IS '생성 시각';
COMMENT ON COLUMN "entry_tags"."updated_at" IS '사용자 확인/수정 시각';
CREATE INDEX "ix_entry_tags_tag_confirmed" ON "entry_tags" ("tag_id", "entry_id") WHERE state IN ('CONFIRMED', 'SYSTEM');
CREATE INDEX "ix_entry_tags_entry_state" ON "entry_tags" ("entry_id", "state");
CREATE TRIGGER "trg_entry_tags_updated_at" BEFORE UPDATE ON "entry_tags" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "entry_self_reflections" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
    "ai_reflection_id" uuid,
    "content" text NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "entry_self_reflections" IS 'AI 해석과 분리해 저장하는 사용자의 추가 생각';
COMMENT ON COLUMN "entry_self_reflections"."id" IS '내 생각 ID';
COMMENT ON COLUMN "entry_self_reflections"."entry_id" IS '대상 기록';
COMMENT ON COLUMN "entry_self_reflections"."ai_reflection_id" IS '생각을 덧붙이게 한 AI 정리; AI 도메인 구현 시 FK 추가';
COMMENT ON COLUMN "entry_self_reflections"."content" IS '사용자가 직접 작성한 해석';
COMMENT ON COLUMN "entry_self_reflections"."created_at" IS '생성 시각';
COMMENT ON COLUMN "entry_self_reflections"."updated_at" IS '수정 시각';
CREATE INDEX "ix_entry_self_reflections_entry" ON "entry_self_reflections" ("entry_id", "created_at");
CREATE TRIGGER "trg_entry_self_reflections_updated_at" BEFORE UPDATE ON "entry_self_reflections" FOR EACH ROW EXECUTE FUNCTION set_updated_at();
