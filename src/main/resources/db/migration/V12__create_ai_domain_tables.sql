-- Naroom Beta 1
-- AI 도메인 2단계(데이터 구조). docs/api/ai-policy-architecture.md 16장,
-- docs/instruction/0724_AI-Integration-Review.md 5~10장, 0725_AI-Domain-Schema-Review-Report.md
-- 기준으로 AI 전용 신규 테이블 11개를 생성한다. 기존 도메인 스키마는 건드리지 않는다.
--
-- 문장형 컬럼(reflection_text, content 등)은 이번 마이그레이션에서 평문으로 둔다.
-- 필드 암호화 도입 시점은 별도 결정 사항이라 컬럼명에 _ciphertext를 아직 붙이지 않는다.
--
-- ai_generation_runs가 모델/토큰/프롬프트 버전/안전 분류 이력을 전담하고,
-- ai_reflections/ai_conversations/weekly_reflections 등은 사용자 표시 결과만 담당한다
-- (0724 리뷰 5장 "AI 결과와 모델 호출 이력 분리" 결정).
--
-- 공통 지침 버전과 기능별 지침 버전을 각각 추적해야 하므로(정책 문서 14.6),
-- ai_prompt_versions는 scope(COMMON/FEATURE)로 구분하고 ai_generation_runs는
-- common_prompt_version_id/feature_prompt_version_id 두 개를 따로 참조한다.
--
-- weekly_reflection_id는 LifeTime 도메인의 weekly_reflections 테이블이 아직 없어
-- ai_jobs에서 이번엔 제외한다(해당 도메인 마이그레이션 작성 시 추가).

CREATE TYPE "ai_feature_type" AS ENUM (
    'ENTRY_REFLECTION',
    'THREE_DAY_REFLECTION',
    'WEEKLY_REFLECTION',
    'CONVERSATION_REPLY',
    'CONVERSATION_SUMMARY'
);

CREATE TYPE "ai_job_status" AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'BLOCKED',
    'SAFETY_SUPPORT',
    'FAILED'
);

CREATE TYPE "ai_safety_grade" AS ENUM ('NORMAL', 'RESTRICTED', 'CRISIS', 'BLOCKED_OUTPUT');

CREATE TYPE "ai_message_role" AS ENUM ('USER', 'ASSISTANT');

CREATE TYPE "ai_feedback_helpfulness" AS ENUM ('HELPFUL', 'SOMEWHAT_UNHELPFUL', 'UNHELPFUL');

CREATE TYPE "ai_prompt_scope" AS ENUM ('COMMON', 'FEATURE');

-- ---------------------------------------------------------------------------
-- 후속 대화
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_conversations" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "feature_type" ai_feature_type NOT NULL,
    "source_entry_id" uuid REFERENCES "entries" ("id") ON DELETE CASCADE,
    "started_at" timestamptz NOT NULL DEFAULT now(),
    "last_message_at" timestamptz NOT NULL DEFAULT now(),
    "closed_at" timestamptz
);
COMMENT ON TABLE "ai_conversations" IS '기록·회고에서 사용자가 직접 시작한 후속 대화 단위; 자동 생성되지 않음';
COMMENT ON COLUMN "ai_conversations"."id" IS '대화 ID';
COMMENT ON COLUMN "ai_conversations"."member_id" IS '대화 소유 회원';
COMMENT ON COLUMN "ai_conversations"."feature_type" IS '이 대화의 출발점이 된 회고 기능(개별/3일/주간)';
COMMENT ON COLUMN "ai_conversations"."source_entry_id" IS '대화를 시작하게 한 기록·회고';
COMMENT ON COLUMN "ai_conversations"."started_at" IS '대화 시작 시각';
COMMENT ON COLUMN "ai_conversations"."last_message_at" IS '마지막 메시지 시각; 목록 정렬용';
COMMENT ON COLUMN "ai_conversations"."closed_at" IS '대화 종료 시각';
CREATE INDEX "ix_ai_conversations_member_recent" ON "ai_conversations" ("member_id", "last_message_at");

-- ---------------------------------------------------------------------------
-- 프롬프트 버전
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_prompt_versions" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "scope" ai_prompt_scope NOT NULL,
    "feature_type" ai_feature_type,
    "version_label" varchar(40) NOT NULL,
    "output_schema_version" varchar(40),
    "is_active" boolean NOT NULL DEFAULT true,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "ck_ai_prompt_versions_1" CHECK (
        (scope = 'COMMON' AND feature_type IS NULL) OR (scope = 'FEATURE' AND feature_type IS NOT NULL)
    )
);
COMMENT ON TABLE "ai_prompt_versions" IS '공통 지침과 기능별 지침·출력 스키마의 버전 이력';
COMMENT ON COLUMN "ai_prompt_versions"."id" IS '프롬프트 버전 ID';
COMMENT ON COLUMN "ai_prompt_versions"."scope" IS '공통 지침인지 기능별 지침인지 구분';
COMMENT ON COLUMN "ai_prompt_versions"."feature_type" IS 'FEATURE 범위일 때만 값이 있음';
COMMENT ON COLUMN "ai_prompt_versions"."version_label" IS '지침 버전 식별자(백엔드 코드·관리 파일 기준)';
COMMENT ON COLUMN "ai_prompt_versions"."output_schema_version" IS 'FEATURE 범위의 출력 JSON Schema 버전';
COMMENT ON COLUMN "ai_prompt_versions"."is_active" IS '신규 호출에 사용 중인 버전 여부';
COMMENT ON COLUMN "ai_prompt_versions"."created_at" IS '버전 등록 시각';
CREATE UNIQUE INDEX "uq_ai_prompt_versions_common" ON "ai_prompt_versions" ("version_label") WHERE scope = 'COMMON';
CREATE UNIQUE INDEX "uq_ai_prompt_versions_feature" ON "ai_prompt_versions" ("feature_type", "version_label") WHERE scope = 'FEATURE';

-- ---------------------------------------------------------------------------
-- 비동기 작업
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_jobs" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "feature_type" ai_feature_type NOT NULL,
    "entry_id" uuid REFERENCES "entries" ("id") ON DELETE CASCADE,
    "conversation_id" uuid REFERENCES "ai_conversations" ("id") ON DELETE CASCADE,
    "status" ai_job_status NOT NULL DEFAULT 'PENDING',
    "idempotency_key" varchar(100) NOT NULL,
    "attempt_count" integer NOT NULL DEFAULT 0,
    "max_attempts" integer NOT NULL DEFAULT 3,
    "next_retry_at" timestamptz,
    "started_at" timestamptz,
    "completed_at" timestamptz,
    "error_code" varchar(80),
    "created_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("member_id", "idempotency_key")
);
COMMENT ON TABLE "ai_jobs" IS '비동기 AI 작업의 상태·재시도·멱등성 관리';
COMMENT ON COLUMN "ai_jobs"."id" IS '작업 ID';
COMMENT ON COLUMN "ai_jobs"."member_id" IS '요청 회원';
COMMENT ON COLUMN "ai_jobs"."feature_type" IS '처리할 AI 기능 유형';
COMMENT ON COLUMN "ai_jobs"."entry_id" IS '개별 기록·3일 회고 대상 기록';
COMMENT ON COLUMN "ai_jobs"."conversation_id" IS '후속 대화 응답·요약 대상 대화';
COMMENT ON COLUMN "ai_jobs"."status" IS '처리 상태';
COMMENT ON COLUMN "ai_jobs"."idempotency_key" IS '동일 요청 중복 전송 차단 키';
COMMENT ON COLUMN "ai_jobs"."attempt_count" IS '시스템 재시도 누적 횟수';
COMMENT ON COLUMN "ai_jobs"."max_attempts" IS '허용 최대 재시도 횟수';
COMMENT ON COLUMN "ai_jobs"."next_retry_at" IS '다음 재시도 예정 시각';
COMMENT ON COLUMN "ai_jobs"."started_at" IS '처리 시작 시각';
COMMENT ON COLUMN "ai_jobs"."completed_at" IS '처리 종료 시각';
COMMENT ON COLUMN "ai_jobs"."error_code" IS '실패 유형; 원문·비밀정보 미포함';
COMMENT ON COLUMN "ai_jobs"."created_at" IS '작업 생성 시각';
CREATE INDEX "ix_ai_jobs_queue" ON "ai_jobs" ("status", "next_retry_at") WHERE status IN ('PENDING', 'PROCESSING', 'FAILED');
CREATE INDEX "ix_ai_jobs_member_recent" ON "ai_jobs" ("member_id", "created_at");

-- ---------------------------------------------------------------------------
-- 모델 호출 이력
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_generation_runs" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "ai_job_id" uuid NOT NULL REFERENCES "ai_jobs" ("id") ON DELETE CASCADE,
    "model_name" varchar(80) NOT NULL,
    "common_prompt_version_id" uuid NOT NULL REFERENCES "ai_prompt_versions" ("id"),
    "feature_prompt_version_id" uuid NOT NULL REFERENCES "ai_prompt_versions" ("id"),
    "output_schema_version" varchar(40) NOT NULL,
    "input_tokens" integer,
    "output_tokens" integer,
    "input_safety_status" ai_safety_grade,
    "output_safety_status" ai_safety_grade,
    "latency_ms" integer,
    "store_enabled" boolean NOT NULL DEFAULT false,
    "is_regeneration" boolean NOT NULL DEFAULT false,
    "parent_run_id" uuid REFERENCES "ai_generation_runs" ("id"),
    "requested_at" timestamptz NOT NULL DEFAULT now(),
    "completed_at" timestamptz
);
COMMENT ON TABLE "ai_generation_runs" IS '실제 모델 호출 1회의 이력; 모델·토큰·프롬프트 버전·안전 분류 결과 전담';
COMMENT ON COLUMN "ai_generation_runs"."id" IS '호출 이력 ID';
COMMENT ON COLUMN "ai_generation_runs"."ai_job_id" IS '이 호출을 발생시킨 작업';
COMMENT ON COLUMN "ai_generation_runs"."model_name" IS '호출 모델 식별자';
COMMENT ON COLUMN "ai_generation_runs"."common_prompt_version_id" IS '적용된 공통 지침 버전';
COMMENT ON COLUMN "ai_generation_runs"."feature_prompt_version_id" IS '적용된 기능별 지침 버전';
COMMENT ON COLUMN "ai_generation_runs"."output_schema_version" IS '이 호출에 실제 적용된 출력 스키마 버전';
COMMENT ON COLUMN "ai_generation_runs"."input_tokens" IS '입력 토큰 수';
COMMENT ON COLUMN "ai_generation_runs"."output_tokens" IS '출력 토큰 수';
COMMENT ON COLUMN "ai_generation_runs"."input_safety_status" IS '입력 Moderation 분류 결과';
COMMENT ON COLUMN "ai_generation_runs"."output_safety_status" IS '출력 Moderation 분류 결과';
COMMENT ON COLUMN "ai_generation_runs"."latency_ms" IS '호출 처리 시간';
COMMENT ON COLUMN "ai_generation_runs"."store_enabled" IS 'OpenAI 측 응답 저장 여부; 기본 false';
COMMENT ON COLUMN "ai_generation_runs"."is_regeneration" IS '사용자 재생성 요청으로 인한 호출인지 여부';
COMMENT ON COLUMN "ai_generation_runs"."parent_run_id" IS '재생성 시 원본 호출';
COMMENT ON COLUMN "ai_generation_runs"."requested_at" IS '호출 요청 시각';
COMMENT ON COLUMN "ai_generation_runs"."completed_at" IS '호출 완료 시각';
CREATE INDEX "ix_ai_generation_runs_job" ON "ai_generation_runs" ("ai_job_id");

-- ---------------------------------------------------------------------------
-- 개별 기록 AI 정리
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_reflections" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
    "generation_run_id" uuid REFERENCES "ai_generation_runs" ("id"),
    "version_no" integer NOT NULL DEFAULT 1,
    "status" ai_job_status NOT NULL DEFAULT 'PENDING',
    "reflection_text" text,
    "question_text" text,
    "structured_result" jsonb,
    "safety_code" varchar(50),
    "hidden_at" timestamptz,
    "error_code" varchar(80),
    "requested_at" timestamptz NOT NULL DEFAULT now(),
    "completed_at" timestamptz,
    UNIQUE ("entry_id", "version_no")
);
COMMENT ON TABLE "ai_reflections" IS '개별 기록에 대한 사용자 표시용 AI 정리 결과; 모델·토큰 정보는 ai_generation_runs 참고';
COMMENT ON COLUMN "ai_reflections"."id" IS 'AI 정리 ID';
COMMENT ON COLUMN "ai_reflections"."entry_id" IS '대상 기록';
COMMENT ON COLUMN "ai_reflections"."generation_run_id" IS '이 결과를 생성한 모델 호출 이력';
COMMENT ON COLUMN "ai_reflections"."version_no" IS '재생성 포함 기록별 버전';
COMMENT ON COLUMN "ai_reflections"."status" IS '처리 상태';
COMMENT ON COLUMN "ai_reflections"."reflection_text" IS '사용자에게 표시할 AI 반영문';
COMMENT ON COLUMN "ai_reflections"."question_text" IS '사용자가 생각해볼 후속 질문 한 가지';
COMMENT ON COLUMN "ai_reflections"."structured_result" IS '감정·태그 후보 등 버전 가능한 구조화 결과';
COMMENT ON COLUMN "ai_reflections"."safety_code" IS '사용자에게 노출되는 안전 분기 코드';
COMMENT ON COLUMN "ai_reflections"."hidden_at" IS '사용자가 이 AI 정리를 숨긴 시각';
COMMENT ON COLUMN "ai_reflections"."error_code" IS '실패 유형; 원문·비밀정보 미포함';
COMMENT ON COLUMN "ai_reflections"."requested_at" IS 'AI 요청 시각';
COMMENT ON COLUMN "ai_reflections"."completed_at" IS 'AI 처리 완료 시각';
CREATE INDEX "ix_ai_reflections_entry_latest" ON "ai_reflections" ("entry_id", "version_no");

-- entry_self_reflections.ai_reflection_id는 V5부터 "AI 도메인 구현 시 FK 추가" 예정이던 컬럼이다.
ALTER TABLE "entry_self_reflections"
    ADD CONSTRAINT "fk_entry_self_reflections_ai_reflection"
    FOREIGN KEY ("ai_reflection_id") REFERENCES "ai_reflections" ("id") ON DELETE SET NULL;
COMMENT ON COLUMN "entry_self_reflections"."ai_reflection_id" IS '생각을 덧붙이게 한 AI 정리';

-- ---------------------------------------------------------------------------
-- 후속 대화 메시지
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_messages" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "conversation_id" uuid NOT NULL REFERENCES "ai_conversations" ("id") ON DELETE CASCADE,
    "generation_run_id" uuid REFERENCES "ai_generation_runs" ("id"),
    "role" ai_message_role NOT NULL,
    "content" text NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "ai_messages" IS '후속 대화의 사용자·AI 메시지 원문';
COMMENT ON COLUMN "ai_messages"."id" IS '메시지 ID';
COMMENT ON COLUMN "ai_messages"."conversation_id" IS '소속 대화';
COMMENT ON COLUMN "ai_messages"."generation_run_id" IS 'AI 메시지를 생성한 모델 호출 이력; 사용자 메시지는 NULL';
COMMENT ON COLUMN "ai_messages"."role" IS '작성 주체';
COMMENT ON COLUMN "ai_messages"."content" IS '메시지 원문';
COMMENT ON COLUMN "ai_messages"."created_at" IS '작성 시각';
CREATE INDEX "ix_ai_messages_conversation_order" ON "ai_messages" ("conversation_id", "created_at");

CREATE TABLE "ai_conversation_summaries" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "conversation_id" uuid NOT NULL REFERENCES "ai_conversations" ("id") ON DELETE CASCADE,
    "summary_text" text NOT NULL,
    "covers_until_message_id" uuid NOT NULL REFERENCES "ai_messages" ("id") ON DELETE RESTRICT,
    "created_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "ai_conversation_summaries" IS '길어진 대화의 압축 맥락; 원문을 대체하지 않음';
COMMENT ON COLUMN "ai_conversation_summaries"."id" IS '요약 ID';
COMMENT ON COLUMN "ai_conversation_summaries"."conversation_id" IS '소속 대화';
COMMENT ON COLUMN "ai_conversation_summaries"."summary_text" IS '압축된 맥락 요약';
COMMENT ON COLUMN "ai_conversation_summaries"."covers_until_message_id" IS '이 요약이 포함하는 마지막 메시지';
COMMENT ON COLUMN "ai_conversation_summaries"."created_at" IS '요약 생성 시각';
CREATE INDEX "ix_ai_conversation_summaries_conversation" ON "ai_conversation_summaries" ("conversation_id", "created_at");

-- ---------------------------------------------------------------------------
-- 신고·만족도·선호
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_feedback_reports" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "ai_generation_run_id" uuid NOT NULL REFERENCES "ai_generation_runs" ("id") ON DELETE CASCADE,
    "reason_code" varchar(50) NOT NULL,
    "comment" text,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("member_id", "ai_generation_run_id")
);
COMMENT ON TABLE "ai_feedback_reports" IS '부적절·위험·오류 AI 응답 신고; 기록 원문을 중복 저장하지 않음';
COMMENT ON COLUMN "ai_feedback_reports"."id" IS '신고 ID';
COMMENT ON COLUMN "ai_feedback_reports"."member_id" IS '신고 회원';
COMMENT ON COLUMN "ai_feedback_reports"."ai_generation_run_id" IS '신고 대상 모델 호출 이력; 개별/회고/대화 응답 공통 참조';
COMMENT ON COLUMN "ai_feedback_reports"."reason_code" IS '부적절·단정·위험 등 신고 사유 코드';
COMMENT ON COLUMN "ai_feedback_reports"."comment" IS '선택 상세 설명';
COMMENT ON COLUMN "ai_feedback_reports"."created_at" IS '신고 시각';
CREATE INDEX "ix_ai_feedback_reports_created" ON "ai_feedback_reports" ("created_at");

CREATE TABLE "ai_feedback" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "ai_generation_run_id" uuid NOT NULL REFERENCES "ai_generation_runs" ("id") ON DELETE CASCADE,
    "helpfulness" ai_feedback_helpfulness NOT NULL,
    "reason_code" varchar(50),
    "custom_reason" text,
    "apply_long_term" boolean,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("member_id", "ai_generation_run_id")
);
COMMENT ON TABLE "ai_feedback" IS 'AI 결과가 도움이 되었는지에 대한 만족도; 신고와는 별개';
COMMENT ON COLUMN "ai_feedback"."id" IS '만족도 평가 ID';
COMMENT ON COLUMN "ai_feedback"."member_id" IS '평가 회원';
COMMENT ON COLUMN "ai_feedback"."ai_generation_run_id" IS '평가 대상 모델 호출 이력';
COMMENT ON COLUMN "ai_feedback"."helpfulness" IS '1차 평가: 도움됨/조금 아쉬움/도움 안 됨';
COMMENT ON COLUMN "ai_feedback"."reason_code" IS '2차 사유 코드; 부정 평가일 때만 값이 있음';
COMMENT ON COLUMN "ai_feedback"."custom_reason" IS '2차 사유의 직접 입력; 서버 정규화 전 원문';
COMMENT ON COLUMN "ai_feedback"."apply_long_term" IS '이 평가를 다음 AI 회고에도 반영할지 여부';
COMMENT ON COLUMN "ai_feedback"."created_at" IS '평가 생성 시각';
COMMENT ON COLUMN "ai_feedback"."updated_at" IS '2차 사유·장기 반영 확인 등 이후 수정 시각';
CREATE TRIGGER "trg_ai_feedback_updated_at" BEFORE UPDATE ON "ai_feedback" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "member_ai_preferences" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL UNIQUE REFERENCES "members" ("id") ON DELETE CASCADE,
    "tone" varchar(30),
    "response_length" varchar(30),
    "reduce_emotional_validation" boolean NOT NULL DEFAULT false,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "member_ai_preferences" IS '사용자가 명시적으로 확인·확정한 AI 응답 선호; 자유 입력을 정규화해 저장';
COMMENT ON COLUMN "member_ai_preferences"."id" IS '선호 설정 ID';
COMMENT ON COLUMN "member_ai_preferences"."member_id" IS '설정 소유 회원';
COMMENT ON COLUMN "member_ai_preferences"."tone" IS '선호 말투(예: DIRECT); 허용 값은 애플리케이션에서 검증';
COMMENT ON COLUMN "member_ai_preferences"."response_length" IS '선호 응답 길이(예: SHORT); 허용 값은 애플리케이션에서 검증';
COMMENT ON COLUMN "member_ai_preferences"."reduce_emotional_validation" IS '정서적 공감 표현을 줄이고 핵심 위주로 응답할지 여부';
COMMENT ON COLUMN "member_ai_preferences"."created_at" IS '최초 설정 시각';
COMMENT ON COLUMN "member_ai_preferences"."updated_at" IS '수정 시각';
CREATE TRIGGER "trg_member_ai_preferences_updated_at" BEFORE UPDATE ON "member_ai_preferences" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- 운영 집계
-- ---------------------------------------------------------------------------

CREATE TABLE "ai_usage_daily" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "usage_date" date NOT NULL,
    "feature_type" ai_feature_type NOT NULL,
    "model_name" varchar(80) NOT NULL,
    "call_count" integer NOT NULL DEFAULT 0,
    "regeneration_count" integer NOT NULL DEFAULT 0,
    "input_tokens" bigint NOT NULL DEFAULT 0,
    "output_tokens" bigint NOT NULL DEFAULT 0,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    UNIQUE ("member_id", "usage_date", "feature_type", "model_name")
);
COMMENT ON TABLE "ai_usage_daily" IS '회원·기능·모델별 일일 호출·토큰 집계; ai_generation_runs에서 계산되는 파생 테이블';
COMMENT ON COLUMN "ai_usage_daily"."id" IS '집계 ID';
COMMENT ON COLUMN "ai_usage_daily"."member_id" IS '집계 대상 회원';
COMMENT ON COLUMN "ai_usage_daily"."usage_date" IS '회원 시간대 기준 집계 날짜';
COMMENT ON COLUMN "ai_usage_daily"."feature_type" IS '집계 대상 기능 유형';
COMMENT ON COLUMN "ai_usage_daily"."model_name" IS '집계 대상 모델';
COMMENT ON COLUMN "ai_usage_daily"."call_count" IS '호출 횟수';
COMMENT ON COLUMN "ai_usage_daily"."regeneration_count" IS '재생성 횟수';
COMMENT ON COLUMN "ai_usage_daily"."input_tokens" IS '입력 토큰 누적';
COMMENT ON COLUMN "ai_usage_daily"."output_tokens" IS '출력 토큰 누적';
COMMENT ON COLUMN "ai_usage_daily"."created_at" IS '집계 행 생성 시각';
COMMENT ON COLUMN "ai_usage_daily"."updated_at" IS '집계 갱신 시각';
CREATE TRIGGER "trg_ai_usage_daily_updated_at" BEFORE UPDATE ON "ai_usage_daily" FOR EACH ROW EXECUTE FUNCTION set_updated_at();
