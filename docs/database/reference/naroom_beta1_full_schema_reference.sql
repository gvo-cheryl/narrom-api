-- Naroom Beta 1 - PostgreSQL 16 reference schema
-- Scope: Beta 1 implementation tables; P1/P2 are extension foundations only.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE "member_status" AS ENUM ('ACTIVE', 'LOCKED', 'PENDING_DELETION');
CREATE TYPE "social_provider" AS ENUM ('KAKAO', 'GOOGLE');
CREATE TYPE "identity_status" AS ENUM ('ACTIVE', 'REVOKED');
CREATE TYPE "consent_type" AS ENUM ('TERMS', 'PRIVACY', 'AI_PROCESSING');
CREATE TYPE "notification_type" AS ENUM ('WEEKLY_REFLECTION', 'EXPERIMENT_MISSION', 'DAILY_QUOTE');
CREATE TYPE "entry_type" AS ENUM ('FREE', 'CHECK_IN', 'GRATITUDE', 'EMOTION', 'PROMPT', 'QUOTE_REFLECTION', 'EXPERIMENT_MISSION', 'EXPERIMENT_REVIEW', 'WEEKLY_REFLECTION', 'SELF_SUMMARY');
CREATE TYPE "entry_status" AS ENUM ('DRAFT', 'PUBLISHED');
CREATE TYPE "tag_category" AS ENUM ('EMOTION', 'SITUATION', 'NEED', 'VALUE', 'ACTION', 'RECOVERY', 'CUSTOM');
CREATE TYPE "tag_scope" AS ENUM ('SYSTEM', 'USER');
CREATE TYPE "tag_source" AS ENUM ('USER', 'AI', 'CHECK_IN', 'REFLECTION', 'EXPERIMENT');
CREATE TYPE "tag_state" AS ENUM ('SUGGESTED', 'CONFIRMED', 'REJECTED', 'SYSTEM');
CREATE TYPE "ai_job_status" AS ENUM ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'BLOCKED');
CREATE TYPE "ai_user_feedback" AS ENUM ('RESONATES', 'DIFFERENT', 'UNSURE');
CREATE TYPE "weekly_reflection_status" AS ENUM ('DRAFT', 'GENERATING', 'READY', 'FAILED');
CREATE TYPE "quote_status" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE "experiment_source_type" AS ENUM ('TEMPLATE', 'RANDOM', 'AI_RECOMMENDED', 'USER_COMPOSED');
CREATE TYPE "content_status" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE "mission_type" AS ENUM ('OBSERVATION', 'QUESTION', 'ACTION', 'RECORD', 'REVIEW');
CREATE TYPE "user_program_status" AS ENUM ('READY', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'ENDED_EARLY');
CREATE TYPE "user_mission_status" AS ENUM ('SCHEDULED', 'AVAILABLE', 'RECORDED');
CREATE TYPE "attempt_status" AS ENUM ('DONE', 'PARTIALLY_DONE', 'RESTED', 'TRIED_DIFFERENTLY', 'NOT_A_FIT', 'RECORD_ONLY');
CREATE TYPE "recommendation_source" AS ENUM ('DEFAULT', 'RECORD', 'CHECK_IN', 'WEEKLY_REFLECTION', 'RANDOM', 'AI');
CREATE TYPE "recommendation_status" AS ENUM ('SUGGESTED', 'ACCEPTED', 'DISMISSED', 'EXPIRED');
CREATE TYPE "summary_scope" AS ENUM ('CURRENT_SELF', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'EXPERIMENT');

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

CREATE TABLE "members" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "display_name" varchar(80) NOT NULL,
  "status" member_status NOT NULL DEFAULT 'ACTIVE',
  "timezone" varchar(50) NOT NULL DEFAULT 'Asia/Seoul',
  "locale" varchar(10) NOT NULL DEFAULT 'ko-KR',
  "onboarding_completed_at" timestamptz,
  "withdrawal_requested_at" timestamptz,
  "scheduled_deletion_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  CONSTRAINT "ck_members_1" CHECK ((status = 'PENDING_DELETION') = (scheduled_deletion_at IS NOT NULL))
);
COMMENT ON TABLE "members" IS '소셜 제공자와 분리된 나로움 내부 회원';
COMMENT ON COLUMN "members"."id" IS '나로움 내부 회원 ID';
COMMENT ON COLUMN "members"."display_name" IS '앱에 표시할 닉네임';
COMMENT ON COLUMN "members"."status" IS '현재 회원 상태';
COMMENT ON COLUMN "members"."timezone" IS 'IANA 시간대';
COMMENT ON COLUMN "members"."locale" IS '표시 언어/지역';
COMMENT ON COLUMN "members"."onboarding_completed_at" IS '온보딩 완료 시각';
COMMENT ON COLUMN "members"."withdrawal_requested_at" IS '탈퇴 요청 시각';
COMMENT ON COLUMN "members"."scheduled_deletion_at" IS '영구 삭제 예정 시각';
COMMENT ON COLUMN "members"."created_at" IS '생성 시각';
COMMENT ON COLUMN "members"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "members"."version" IS 'JPA 낙관적 잠금 버전';
CREATE INDEX "ix_members_deletion_due" ON "members" ("scheduled_deletion_at") WHERE status = 'PENDING_DELETION';
CREATE TRIGGER "trg_members_updated_at" BEFORE UPDATE ON "members" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "social_identities" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "provider" social_provider NOT NULL,
  "provider_user_id" varchar(255) NOT NULL,
  "email" varchar(320),
  "email_verified" boolean NOT NULL DEFAULT false,
  "profile_name" varchar(100),
  "profile_image_url" text,
  "status" identity_status NOT NULL DEFAULT 'ACTIVE',
  "connected_at" timestamptz NOT NULL DEFAULT now(),
  "last_login_at" timestamptz,
  "disconnected_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("provider", "provider_user_id")
);
COMMENT ON TABLE "social_identities" IS '회원과 외부 소셜 계정 식별자의 연결';
COMMENT ON COLUMN "social_identities"."id" IS '소셜 식별자 행 ID';
COMMENT ON COLUMN "social_identities"."member_id" IS '소유 회원';
COMMENT ON COLUMN "social_identities"."provider" IS '소셜 제공자';
COMMENT ON COLUMN "social_identities"."provider_user_id" IS '카카오 서비스 사용자 ID 또는 Google sub';
COMMENT ON COLUMN "social_identities"."email" IS '제공된 이메일; 계정 후보 탐색용';
COMMENT ON COLUMN "social_identities"."email_verified" IS '제공자가 검증한 이메일인지 여부';
COMMENT ON COLUMN "social_identities"."profile_name" IS '소셜 프로필 이름';
COMMENT ON COLUMN "social_identities"."profile_image_url" IS '소셜 프로필 이미지 URL';
COMMENT ON COLUMN "social_identities"."status" IS '현재 외부 연결 상태';
COMMENT ON COLUMN "social_identities"."connected_at" IS '최초 연결 시각';
COMMENT ON COLUMN "social_identities"."last_login_at" IS '마지막 로그인 시각';
COMMENT ON COLUMN "social_identities"."disconnected_at" IS '외부 권한 취소 등으로 비활성화된 시각';
COMMENT ON COLUMN "social_identities"."created_at" IS '생성 시각';
COMMENT ON COLUMN "social_identities"."updated_at" IS '수정 시각';
CREATE INDEX "ix_social_identities_member" ON "social_identities" ("member_id");
CREATE INDEX "ix_social_identities_verified_email" ON "social_identities" (lower(email)) WHERE email_verified = true AND email IS NOT NULL;
CREATE TRIGGER "trg_social_identities_updated_at" BEFORE UPDATE ON "social_identities" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "device_installations" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "installation_key" varchar(255) NOT NULL,
  "platform" varchar(30) NOT NULL,
  "push_token_ciphertext" text,
  "app_version" varchar(30),
  "last_seen_at" timestamptz NOT NULL DEFAULT now(),
  "revoked_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("installation_key")
);
COMMENT ON TABLE "device_installations" IS '푸시 알림과 세션 기기 단위 관리를 위한 앱 설치 정보';
COMMENT ON COLUMN "device_installations"."id" IS '기기 설치 ID';
COMMENT ON COLUMN "device_installations"."member_id" IS '소유 회원';
COMMENT ON COLUMN "device_installations"."installation_key" IS '앱 설치 단위 난수 식별자';
COMMENT ON COLUMN "device_installations"."platform" IS 'IOS, ANDROID, WEB 등 클라이언트 플랫폼';
COMMENT ON COLUMN "device_installations"."push_token_ciphertext" IS '애플리케이션 계층에서 암호화한 푸시 토큰';
COMMENT ON COLUMN "device_installations"."app_version" IS '마지막 확인 앱 버전';
COMMENT ON COLUMN "device_installations"."last_seen_at" IS '마지막 앱 사용 시각';
COMMENT ON COLUMN "device_installations"."revoked_at" IS '알림/기기 연결 해제 시각';
COMMENT ON COLUMN "device_installations"."created_at" IS '생성 시각';
COMMENT ON COLUMN "device_installations"."updated_at" IS '수정 시각';
CREATE INDEX "ix_device_installations_member" ON "device_installations" ("member_id");
CREATE TRIGGER "trg_device_installations_updated_at" BEFORE UPDATE ON "device_installations" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "auth_sessions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "device_installation_id" uuid REFERENCES "device_installations" ("id") ON DELETE SET NULL,
  "refresh_token_hash" varchar(128) NOT NULL UNIQUE,
  "issued_at" timestamptz NOT NULL DEFAULT now(),
  "expires_at" timestamptz NOT NULL,
  "last_used_at" timestamptz,
  "revoked_at" timestamptz,
  "revoke_reason" varchar(50)
);
COMMENT ON TABLE "auth_sessions" IS '원문 토큰을 저장하지 않는 서버 세션/리프레시 토큰 관리';
COMMENT ON COLUMN "auth_sessions"."id" IS '세션 ID';
COMMENT ON COLUMN "auth_sessions"."member_id" IS '로그인 회원';
COMMENT ON COLUMN "auth_sessions"."device_installation_id" IS '세션을 만든 설치 기기';
COMMENT ON COLUMN "auth_sessions"."refresh_token_hash" IS '리프레시 토큰 단방향 해시';
COMMENT ON COLUMN "auth_sessions"."issued_at" IS '발급 시각';
COMMENT ON COLUMN "auth_sessions"."expires_at" IS '만료 시각';
COMMENT ON COLUMN "auth_sessions"."last_used_at" IS '마지막 갱신 시각';
COMMENT ON COLUMN "auth_sessions"."revoked_at" IS '로그아웃/탈퇴에 따른 폐기 시각';
COMMENT ON COLUMN "auth_sessions"."revoke_reason" IS 'LOGOUT, WITHDRAWAL, SECURITY 등 폐기 사유';
CREATE INDEX "ix_auth_sessions_member_active" ON "auth_sessions" ("member_id", "expires_at") WHERE revoked_at IS NULL;

CREATE TABLE "member_consents" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "consent_type" consent_type NOT NULL,
  "document_version" varchar(30) NOT NULL,
  "agreed" boolean NOT NULL,
  "agreed_at" timestamptz NOT NULL DEFAULT now(),
  "withdrawn_at" timestamptz,
  "source" varchar(30) NOT NULL,
  UNIQUE ("member_id", "consent_type", "document_version", "agreed_at")
);
COMMENT ON TABLE "member_consents" IS '약관·개인정보·AI 처리 동의의 버전별 이력';
COMMENT ON COLUMN "member_consents"."id" IS '동의 이력 ID';
COMMENT ON COLUMN "member_consents"."member_id" IS '동의 회원';
COMMENT ON COLUMN "member_consents"."consent_type" IS '동의 문서 유형';
COMMENT ON COLUMN "member_consents"."document_version" IS '사용자가 확인한 문서 버전';
COMMENT ON COLUMN "member_consents"."agreed" IS '동의 여부';
COMMENT ON COLUMN "member_consents"."agreed_at" IS '선택 시각';
COMMENT ON COLUMN "member_consents"."withdrawn_at" IS '동의 철회 시각';
COMMENT ON COLUMN "member_consents"."source" IS 'ONBOARDING, SETTINGS 등 입력 화면';
CREATE INDEX "ix_member_consents_latest" ON "member_consents" ("member_id", "consent_type", "agreed_at");

CREATE TABLE "notification_preferences" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "notification_type" notification_type NOT NULL,
  "enabled" boolean NOT NULL DEFAULT false,
  "local_time" time,
  "day_of_week" smallint,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("member_id", "notification_type"),
  CONSTRAINT "ck_notification_preferences_1" CHECK (day_of_week IS NULL OR day_of_week BETWEEN 1 AND 7)
);
COMMENT ON TABLE "notification_preferences" IS '주간 회고·미션·오늘의 문장 알림 설정';
COMMENT ON COLUMN "notification_preferences"."id" IS '알림 설정 ID';
COMMENT ON COLUMN "notification_preferences"."member_id" IS '설정 소유 회원';
COMMENT ON COLUMN "notification_preferences"."notification_type" IS '알림 유형';
COMMENT ON COLUMN "notification_preferences"."enabled" IS '수신 여부';
COMMENT ON COLUMN "notification_preferences"."local_time" IS '회원 시간대 기준 발송 희망 시간';
COMMENT ON COLUMN "notification_preferences"."day_of_week" IS '주간 알림 요일; ISO 1(월)~7(일)';
COMMENT ON COLUMN "notification_preferences"."created_at" IS '생성 시각';
COMMENT ON COLUMN "notification_preferences"."updated_at" IS '수정 시각';
CREATE TRIGGER "trg_notification_preferences_updated_at" BEFORE UPDATE ON "notification_preferences" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "quote_topics" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(50) NOT NULL UNIQUE,
  "name" varchar(80) NOT NULL,
  "active" boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE "quote_topics" IS '오늘의 문장 주제 분류; P1 선호/숨김 설정의 기준';
COMMENT ON COLUMN "quote_topics"."id" IS '문장 주제 ID';
COMMENT ON COLUMN "quote_topics"."code" IS '변하지 않는 주제 코드';
COMMENT ON COLUMN "quote_topics"."name" IS '사용자 표시 주제명';
COMMENT ON COLUMN "quote_topics"."active" IS '사용 여부';

CREATE TABLE "quotes" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "text" text NOT NULL,
  "author_name" varchar(120),
  "source_name" varchar(255),
  "source_url" text,
  "status" quote_status NOT NULL DEFAULT 'DRAFT',
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "quotes" IS '홈·빈 상태·위젯에 노출되는 오늘의 문장 콘텐츠';
COMMENT ON COLUMN "quotes"."id" IS '문장 ID';
COMMENT ON COLUMN "quotes"."text" IS '문장 본문';
COMMENT ON COLUMN "quotes"."author_name" IS '작가/화자명';
COMMENT ON COLUMN "quotes"."source_name" IS '출처 작품/문헌명';
COMMENT ON COLUMN "quotes"."source_url" IS '출처 확인 URL';
COMMENT ON COLUMN "quotes"."status" IS '게시 상태';
COMMENT ON COLUMN "quotes"."created_at" IS '생성 시각';
COMMENT ON COLUMN "quotes"."updated_at" IS '수정 시각';
CREATE INDEX "ix_quotes_published" ON "quotes" ("created_at") WHERE status = 'PUBLISHED';
CREATE TRIGGER "trg_quotes_updated_at" BEFORE UPDATE ON "quotes" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "quote_topic_links" (
  "quote_id" uuid REFERENCES "quotes" ("id") ON DELETE CASCADE,
  "topic_id" uuid REFERENCES "quote_topics" ("id") ON DELETE CASCADE,
  PRIMARY KEY ("quote_id", "topic_id")
);
COMMENT ON TABLE "quote_topic_links" IS '오늘의 문장과 복수 주제의 다대다 연결';
COMMENT ON COLUMN "quote_topic_links"."quote_id" IS '문장 ID';
COMMENT ON COLUMN "quote_topic_links"."topic_id" IS '주제 ID';

CREATE TABLE "member_saved_quotes" (
  "member_id" uuid REFERENCES "members" ("id") ON DELETE CASCADE,
  "quote_id" uuid REFERENCES "quotes" ("id") ON DELETE CASCADE,
  "saved_at" timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY ("member_id", "quote_id")
);
COMMENT ON TABLE "member_saved_quotes" IS '회원이 저장한 오늘의 문장';
COMMENT ON COLUMN "member_saved_quotes"."member_id" IS '저장 회원';
COMMENT ON COLUMN "member_saved_quotes"."quote_id" IS '저장 문장';
COMMENT ON COLUMN "member_saved_quotes"."saved_at" IS '저장 시각';
CREATE INDEX "ix_member_saved_quotes_recent" ON "member_saved_quotes" ("member_id", "saved_at");

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

CREATE TABLE "check_ins" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
  "check_in_date" date NOT NULL,
  "emotion_intensity" smallint,
  "energy_level" smallint,
  "memorable_event" text,
  "gratitude_note" text,
  "current_need" text,
  "free_note" text,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  UNIQUE ("member_id", "check_in_date"),
  CONSTRAINT "ck_check_ins_1" CHECK (emotion_intensity IS NULL OR emotion_intensity BETWEEN 1 AND 5),
  CONSTRAINT "ck_check_ins_2" CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5)
);
COMMENT ON TABLE "check_ins" IS '하루 1회 기준의 선택형 감정·에너지 체크인';
COMMENT ON COLUMN "check_ins"."id" IS '체크인 ID';
COMMENT ON COLUMN "check_ins"."member_id" IS '체크인 회원';
COMMENT ON COLUMN "check_ins"."entry_id" IS 'LifeTime에 표시할 CHECK_IN 기록 봉투';
COMMENT ON COLUMN "check_ins"."check_in_date" IS '회원 시간대 기준 체크인 날짜';
COMMENT ON COLUMN "check_ins"."emotion_intensity" IS '감정 강도 1~5';
COMMENT ON COLUMN "check_ins"."energy_level" IS '에너지 수준 1~5';
COMMENT ON COLUMN "check_ins"."memorable_event" IS '오늘 마음에 남은 일';
COMMENT ON COLUMN "check_ins"."gratitude_note" IS '감사하거나 다행이었던 일';
COMMENT ON COLUMN "check_ins"."current_need" IS '지금 필요하다고 느끼는 것';
COMMENT ON COLUMN "check_ins"."free_note" IS '추가 자유 기록';
COMMENT ON COLUMN "check_ins"."created_at" IS '생성 시각';
COMMENT ON COLUMN "check_ins"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "check_ins"."version" IS 'JPA 낙관적 잠금 버전';
CREATE TRIGGER "trg_check_ins_updated_at" BEFORE UPDATE ON "check_ins" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "check_in_emotions" (
  "check_in_id" uuid REFERENCES "check_ins" ("id") ON DELETE CASCADE,
  "tag_id" uuid REFERENCES "tags" ("id") ON DELETE RESTRICT,
  "selected_at" timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY ("check_in_id", "tag_id")
);
COMMENT ON TABLE "check_in_emotions" IS '체크인에서 사용자가 직접 선택한 복수 감정';
COMMENT ON COLUMN "check_in_emotions"."check_in_id" IS '체크인 ID';
COMMENT ON COLUMN "check_in_emotions"."tag_id" IS 'EMOTION 분류 태그 ID';
COMMENT ON COLUMN "check_in_emotions"."selected_at" IS '선택 시각';
CREATE INDEX "ix_check_in_emotions_tag" ON "check_in_emotions" ("tag_id", "check_in_id");

CREATE TABLE "entry_tags" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
  "tag_id" uuid NOT NULL REFERENCES "tags" ("id") ON DELETE RESTRICT,
  "source" tag_source NOT NULL,
  "state" tag_state NOT NULL,
  "confidence" numeric(5,4),
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

CREATE TABLE "ai_reflections" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
  "version_no" integer NOT NULL DEFAULT 1,
  "status" ai_job_status NOT NULL DEFAULT 'QUEUED',
  "reflection_text" text,
  "question_text" text,
  "structured_result" jsonb,
  "model_name" varchar(80),
  "prompt_version" varchar(40),
  "safety_code" varchar(50),
  "user_feedback" ai_user_feedback,
  "hidden_at" timestamptz,
  "error_code" varchar(80),
  "requested_at" timestamptz NOT NULL DEFAULT now(),
  "completed_at" timestamptz,
  UNIQUE ("entry_id", "version_no")
);
COMMENT ON TABLE "ai_reflections" IS '기록 원문 저장 이후 비동기로 생성되는 AI 정리와 질문';
COMMENT ON COLUMN "ai_reflections"."id" IS 'AI 정리 ID';
COMMENT ON COLUMN "ai_reflections"."entry_id" IS '대상 기록';
COMMENT ON COLUMN "ai_reflections"."version_no" IS '재생성 포함 기록별 버전';
COMMENT ON COLUMN "ai_reflections"."status" IS '처리 상태';
COMMENT ON COLUMN "ai_reflections"."reflection_text" IS '사용자에게 표시할 AI 반영문';
COMMENT ON COLUMN "ai_reflections"."question_text" IS '사용자가 생각해볼 질문 한 가지';
COMMENT ON COLUMN "ai_reflections"."structured_result" IS '상황·감정·욕구·가치 후보의 버전 가능한 구조';
COMMENT ON COLUMN "ai_reflections"."model_name" IS '호출 모델 식별자';
COMMENT ON COLUMN "ai_reflections"."prompt_version" IS '프롬프트/정책 버전';
COMMENT ON COLUMN "ai_reflections"."safety_code" IS '위험 신호 또는 안전 안내 분기 코드';
COMMENT ON COLUMN "ai_reflections"."user_feedback" IS '공감/다름/모름 사용자 반응';
COMMENT ON COLUMN "ai_reflections"."hidden_at" IS '사용자가 AI 정리를 숨긴 시각';
COMMENT ON COLUMN "ai_reflections"."error_code" IS '실패 유형; 원문/비밀정보 미포함';
COMMENT ON COLUMN "ai_reflections"."requested_at" IS 'AI 요청 시각';
COMMENT ON COLUMN "ai_reflections"."completed_at" IS 'AI 처리 완료 시각';
CREATE INDEX "ix_ai_reflections_queue" ON "ai_reflections" ("status", "requested_at") WHERE status IN ('QUEUED', 'PROCESSING');
CREATE INDEX "ix_ai_reflections_entry_latest" ON "ai_reflections" ("entry_id", "version_no");

CREATE TABLE "entry_self_reflections" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "entry_id" uuid NOT NULL REFERENCES "entries" ("id") ON DELETE CASCADE,
  "ai_reflection_id" uuid REFERENCES "ai_reflections" ("id") ON DELETE SET NULL,
  "content" text NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "entry_self_reflections" IS 'AI 해석과 분리해 저장하는 사용자의 추가 생각';
COMMENT ON COLUMN "entry_self_reflections"."id" IS '내 생각 ID';
COMMENT ON COLUMN "entry_self_reflections"."entry_id" IS '대상 기록';
COMMENT ON COLUMN "entry_self_reflections"."ai_reflection_id" IS '생각을 덧붙이게 한 AI 정리';
COMMENT ON COLUMN "entry_self_reflections"."content" IS '사용자가 직접 작성한 해석';
COMMENT ON COLUMN "entry_self_reflections"."created_at" IS '생성 시각';
COMMENT ON COLUMN "entry_self_reflections"."updated_at" IS '수정 시각';
CREATE INDEX "ix_entry_self_reflections_entry" ON "entry_self_reflections" ("entry_id", "created_at");
CREATE TRIGGER "trg_entry_self_reflections_updated_at" BEFORE UPDATE ON "entry_self_reflections" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "ai_feedback_reports" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "ai_reflection_id" uuid NOT NULL REFERENCES "ai_reflections" ("id") ON DELETE CASCADE,
  "reason_code" varchar(50) NOT NULL,
  "comment" text,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("member_id", "ai_reflection_id")
);
COMMENT ON TABLE "ai_feedback_reports" IS '부적절한 AI 응답 신고; 기록 원문 전체를 중복 저장하지 않음';
COMMENT ON COLUMN "ai_feedback_reports"."id" IS '신고 ID';
COMMENT ON COLUMN "ai_feedback_reports"."member_id" IS '신고 회원';
COMMENT ON COLUMN "ai_feedback_reports"."ai_reflection_id" IS '신고 대상 AI 정리';
COMMENT ON COLUMN "ai_feedback_reports"."reason_code" IS '부적절·단정·위험 등 신고 사유 코드';
COMMENT ON COLUMN "ai_feedback_reports"."comment" IS '선택 상세 설명';
COMMENT ON COLUMN "ai_feedback_reports"."created_at" IS '신고 시각';
CREATE INDEX "ix_ai_feedback_reports_created" ON "ai_feedback_reports" ("created_at");

CREATE TABLE "weekly_reflections" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
  "week_start" date NOT NULL,
  "week_end" date NOT NULL,
  "status" weekly_reflection_status NOT NULL DEFAULT 'DRAFT',
  "ai_summary" text,
  "insights" jsonb,
  "question_text" text,
  "model_name" varchar(80),
  "prompt_version" varchar(40),
  "generated_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("member_id", "week_start"),
  CONSTRAINT "ck_weekly_reflections_1" CHECK (week_end = week_start + 6)
);
COMMENT ON TABLE "weekly_reflections" IS '주간 회고 Lite와 사용자의 한 문장 기록';
COMMENT ON COLUMN "weekly_reflections"."id" IS '주간 회고 ID';
COMMENT ON COLUMN "weekly_reflections"."member_id" IS '회고 회원';
COMMENT ON COLUMN "weekly_reflections"."entry_id" IS 'LifeTime에 표시할 WEEKLY_REFLECTION 기록 봉투';
COMMENT ON COLUMN "weekly_reflections"."week_start" IS 'ISO 주의 시작일';
COMMENT ON COLUMN "weekly_reflections"."week_end" IS '주의 종료일';
COMMENT ON COLUMN "weekly_reflections"."status" IS '생성 상태';
COMMENT ON COLUMN "weekly_reflections"."ai_summary" IS '기록 근거 기반 주간 요약';
COMMENT ON COLUMN "weekly_reflections"."insights" IS '감정·상황·감사·시도·도움 조건의 구조화 결과';
COMMENT ON COLUMN "weekly_reflections"."question_text" IS '이번 주를 살펴볼 질문';
COMMENT ON COLUMN "weekly_reflections"."model_name" IS '생성 모델 식별자';
COMMENT ON COLUMN "weekly_reflections"."prompt_version" IS '프롬프트/정책 버전';
COMMENT ON COLUMN "weekly_reflections"."generated_at" IS 'AI 회고 생성 완료 시각';
COMMENT ON COLUMN "weekly_reflections"."created_at" IS '생성 시각';
COMMENT ON COLUMN "weekly_reflections"."updated_at" IS '수정 시각';
CREATE TRIGGER "trg_weekly_reflections_updated_at" BEFORE UPDATE ON "weekly_reflections" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "weekly_reflection_entries" (
  "weekly_reflection_id" uuid REFERENCES "weekly_reflections" ("id") ON DELETE CASCADE,
  "entry_id" uuid REFERENCES "entries" ("id") ON DELETE CASCADE,
  "evidence_role" varchar(40),
  "linked_at" timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY ("weekly_reflection_id", "entry_id")
);
COMMENT ON TABLE "weekly_reflection_entries" IS '주간 회고의 근거가 된 기록 연결';
COMMENT ON COLUMN "weekly_reflection_entries"."weekly_reflection_id" IS '주간 회고 ID';
COMMENT ON COLUMN "weekly_reflection_entries"."entry_id" IS '근거 기록 ID';
COMMENT ON COLUMN "weekly_reflection_entries"."evidence_role" IS 'EMOTION, GRATITUDE, EFFORT, RECOVERY 등 근거 역할';
COMMENT ON COLUMN "weekly_reflection_entries"."linked_at" IS '연결 시각';
CREATE INDEX "ix_weekly_reflection_entries_entry" ON "weekly_reflection_entries" ("entry_id");

CREATE TABLE "personal_summaries" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
  "scope" summary_scope NOT NULL DEFAULT 'CURRENT_SELF',
  "period_start" date,
  "period_end" date,
  "archived_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT "ck_personal_summaries_1" CHECK (period_end IS NULL OR period_start IS NOT NULL),
  CONSTRAINT "ck_personal_summaries_2" CHECK (period_end IS NULL OR period_end >= period_start)
);
COMMENT ON TABLE "personal_summaries" IS 'AI 분석과 구분되는 사용자의 나의 정리 Lite';
COMMENT ON COLUMN "personal_summaries"."id" IS '자기정리 ID';
COMMENT ON COLUMN "personal_summaries"."member_id" IS '작성 회원';
COMMENT ON COLUMN "personal_summaries"."entry_id" IS 'SELF_SUMMARY 기록 봉투';
COMMENT ON COLUMN "personal_summaries"."scope" IS '현재/주간/월간/분기/실험 범위';
COMMENT ON COLUMN "personal_summaries"."period_start" IS '정리 대상 기간 시작일';
COMMENT ON COLUMN "personal_summaries"."period_end" IS '정리 대상 기간 종료일';
COMMENT ON COLUMN "personal_summaries"."archived_at" IS '과거 관점으로 보관된 시각';
COMMENT ON COLUMN "personal_summaries"."created_at" IS '생성 시각';
COMMENT ON COLUMN "personal_summaries"."updated_at" IS '수정 시각';
CREATE INDEX "ix_personal_summaries_member_recent" ON "personal_summaries" ("member_id", "created_at");
CREATE TRIGGER "trg_personal_summaries_updated_at" BEFORE UPDATE ON "personal_summaries" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "experiment_themes" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(50) NOT NULL UNIQUE,
  "name" varchar(80) NOT NULL,
  "description" text,
  "active" boolean NOT NULL DEFAULT true
);
COMMENT ON TABLE "experiment_themes" IS '감정·관계·휴식 등 작은 실험 탐색 주제';
COMMENT ON COLUMN "experiment_themes"."id" IS '주제 ID';
COMMENT ON COLUMN "experiment_themes"."code" IS '변하지 않는 주제 코드';
COMMENT ON COLUMN "experiment_themes"."name" IS '사용자 표시명';
COMMENT ON COLUMN "experiment_themes"."description" IS '주제 설명';
COMMENT ON COLUMN "experiment_themes"."active" IS '사용 여부';

CREATE TABLE "experiment_programs" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(80) NOT NULL,
  "version_no" integer NOT NULL DEFAULT 1,
  "title" varchar(150) NOT NULL,
  "description" text NOT NULL,
  "duration_days" smallint NOT NULL,
  "estimated_minutes_per_day" smallint,
  "source_type" experiment_source_type NOT NULL,
  "status" content_status NOT NULL DEFAULT 'DRAFT',
  "created_by_member_id" uuid REFERENCES "members" ("id") ON DELETE CASCADE,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("code", "version_no"),
  CONSTRAINT "ck_experiment_programs_1" CHECK (duration_days BETWEEN 1 AND 365),
  CONSTRAINT "ck_experiment_programs_2" CHECK (estimated_minutes_per_day IS NULL OR estimated_minutes_per_day > 0)
);
COMMENT ON TABLE "experiment_programs" IS '3일·7일 중심의 버전 고정 작은 실험 코스 템플릿';
COMMENT ON COLUMN "experiment_programs"."id" IS '코스 버전 ID';
COMMENT ON COLUMN "experiment_programs"."code" IS '코스 계열 코드';
COMMENT ON COLUMN "experiment_programs"."version_no" IS '게시 콘텐츠 버전';
COMMENT ON COLUMN "experiment_programs"."title" IS '코스명';
COMMENT ON COLUMN "experiment_programs"."description" IS '코스 전체 설명';
COMMENT ON COLUMN "experiment_programs"."duration_days" IS '코스 기간; Beta 1 UI는 3·7일만 허용';
COMMENT ON COLUMN "experiment_programs"."estimated_minutes_per_day" IS '하루 예상 소요 시간';
COMMENT ON COLUMN "experiment_programs"."source_type" IS '템플릿/랜덤/AI/사용자 구성';
COMMENT ON COLUMN "experiment_programs"."status" IS '게시 상태';
COMMENT ON COLUMN "experiment_programs"."created_by_member_id" IS 'P1 사용자 직접 구성 시 소유 회원';
COMMENT ON COLUMN "experiment_programs"."created_at" IS '생성 시각';
COMMENT ON COLUMN "experiment_programs"."updated_at" IS '수정 시각';
CREATE INDEX "ix_experiment_programs_discovery" ON "experiment_programs" ("status", "duration_days", "source_type");
CREATE TRIGGER "trg_experiment_programs_updated_at" BEFORE UPDATE ON "experiment_programs" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "experiment_program_themes" (
  "program_id" uuid REFERENCES "experiment_programs" ("id") ON DELETE CASCADE,
  "theme_id" uuid REFERENCES "experiment_themes" ("id") ON DELETE RESTRICT,
  "primary_theme" boolean NOT NULL DEFAULT false,
  PRIMARY KEY ("program_id", "theme_id")
);
COMMENT ON TABLE "experiment_program_themes" IS '코스와 복수 탐색 주제의 연결';
COMMENT ON COLUMN "experiment_program_themes"."program_id" IS '코스 ID';
COMMENT ON COLUMN "experiment_program_themes"."theme_id" IS '주제 ID';
COMMENT ON COLUMN "experiment_program_themes"."primary_theme" IS '대표 주제 여부';
CREATE INDEX "ix_experiment_program_themes_theme" ON "experiment_program_themes" ("theme_id", "program_id");

CREATE TABLE "experiment_missions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(80) NOT NULL,
  "version_no" integer NOT NULL DEFAULT 1,
  "theme_id" uuid REFERENCES "experiment_themes" ("id") ON DELETE SET NULL,
  "title" varchar(150) NOT NULL,
  "description" text NOT NULL,
  "mission_type" mission_type NOT NULL,
  "estimated_minutes" smallint,
  "difficulty_level" smallint,
  "reflection_questions" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "status" content_status NOT NULL DEFAULT 'DRAFT',
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("code", "version_no"),
  CONSTRAINT "ck_experiment_missions_1" CHECK (estimated_minutes IS NULL OR estimated_minutes > 0),
  CONSTRAINT "ck_experiment_missions_2" CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 5),
  CONSTRAINT "ck_experiment_missions_3" CHECK (jsonb_typeof(reflection_questions) = 'array')
);
COMMENT ON TABLE "experiment_missions" IS '하루에 진행하는 버전 고정 질문·관찰·행동·기록 미션';
COMMENT ON COLUMN "experiment_missions"."id" IS '미션 버전 ID';
COMMENT ON COLUMN "experiment_missions"."code" IS '미션 계열 코드';
COMMENT ON COLUMN "experiment_missions"."version_no" IS '게시 콘텐츠 버전';
COMMENT ON COLUMN "experiment_missions"."theme_id" IS '대표 탐색 주제';
COMMENT ON COLUMN "experiment_missions"."title" IS '미션명';
COMMENT ON COLUMN "experiment_missions"."description" IS '진행 안내';
COMMENT ON COLUMN "experiment_missions"."mission_type" IS '관찰/질문/행동/기록/되돌아보기';
COMMENT ON COLUMN "experiment_missions"."estimated_minutes" IS '예상 소요 분';
COMMENT ON COLUMN "experiment_missions"."difficulty_level" IS '교체 추천을 위한 내부 부담도 1~5';
COMMENT ON COLUMN "experiment_missions"."reflection_questions" IS '버전 가능한 선택형 회고 질문 배열';
COMMENT ON COLUMN "experiment_missions"."status" IS '게시 상태';
COMMENT ON COLUMN "experiment_missions"."created_at" IS '생성 시각';
COMMENT ON COLUMN "experiment_missions"."updated_at" IS '수정 시각';
CREATE INDEX "ix_experiment_missions_discovery" ON "experiment_missions" ("status", "theme_id", "mission_type");
CREATE TRIGGER "trg_experiment_missions_updated_at" BEFORE UPDATE ON "experiment_missions" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "program_missions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "program_id" uuid NOT NULL REFERENCES "experiment_programs" ("id") ON DELETE CASCADE,
  "mission_id" uuid NOT NULL REFERENCES "experiment_missions" ("id") ON DELETE RESTRICT,
  "day_number" smallint NOT NULL,
  "display_order" smallint NOT NULL DEFAULT 1,
  "replaceable" boolean NOT NULL DEFAULT true,
  "replacement_group" varchar(80),
  UNIQUE ("program_id", "day_number", "display_order"),
  CONSTRAINT "ck_program_missions_1" CHECK (day_number > 0),
  CONSTRAINT "ck_program_missions_2" CHECK (display_order > 0)
);
COMMENT ON TABLE "program_missions" IS '게시 코스의 날짜·순서별 미션 구성';
COMMENT ON COLUMN "program_missions"."id" IS '코스-미션 연결 ID';
COMMENT ON COLUMN "program_missions"."program_id" IS '코스 ID';
COMMENT ON COLUMN "program_missions"."mission_id" IS '미션 ID';
COMMENT ON COLUMN "program_missions"."day_number" IS '코스 내 Day 번호';
COMMENT ON COLUMN "program_missions"."display_order" IS '같은 Day 내 표시 순서';
COMMENT ON COLUMN "program_missions"."replaceable" IS '사용자 교체 가능 여부';
COMMENT ON COLUMN "program_missions"."replacement_group" IS '대체 후보 묶음 코드';
CREATE INDEX "ix_program_missions_mission" ON "program_missions" ("mission_id");

CREATE TABLE "user_experiment_programs" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "program_id" uuid NOT NULL REFERENCES "experiment_programs" ("id") ON DELETE RESTRICT,
  "status" user_program_status NOT NULL DEFAULT 'READY',
  "title_snapshot" varchar(150) NOT NULL,
  "duration_days_snapshot" smallint NOT NULL,
  "started_at" timestamptz,
  "scheduled_end_date" date,
  "current_day" smallint NOT NULL DEFAULT 1,
  "paused_at" timestamptz,
  "completed_at" timestamptz,
  "ended_early_at" timestamptz,
  "final_review_entry_id" uuid UNIQUE REFERENCES "entries" ("id") ON DELETE SET NULL,
  "review_data" jsonb,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  CONSTRAINT "ck_user_experiment_programs_1" CHECK (duration_days_snapshot > 0),
  CONSTRAINT "ck_user_experiment_programs_2" CHECK (current_day > 0)
);
COMMENT ON TABLE "user_experiment_programs" IS '회원이 실제로 시작하거나 나중에 시작하도록 저장한 코스 인스턴스';
COMMENT ON COLUMN "user_experiment_programs"."id" IS '회원 코스 ID';
COMMENT ON COLUMN "user_experiment_programs"."member_id" IS '진행 회원';
COMMENT ON COLUMN "user_experiment_programs"."program_id" IS '시작한 코스 버전';
COMMENT ON COLUMN "user_experiment_programs"."status" IS '진행 상태';
COMMENT ON COLUMN "user_experiment_programs"."title_snapshot" IS '시작 당시 코스명 사본';
COMMENT ON COLUMN "user_experiment_programs"."duration_days_snapshot" IS '시작 당시 기간 사본';
COMMENT ON COLUMN "user_experiment_programs"."started_at" IS '실제 시작 시각';
COMMENT ON COLUMN "user_experiment_programs"."scheduled_end_date" IS '현재 휴식/일정 반영 종료 예정일';
COMMENT ON COLUMN "user_experiment_programs"."current_day" IS '현재 안내할 Day 번호';
COMMENT ON COLUMN "user_experiment_programs"."paused_at" IS '현재 일시중지 시작 시각';
COMMENT ON COLUMN "user_experiment_programs"."completed_at" IS '정상 마무리 시각';
COMMENT ON COLUMN "user_experiment_programs"."ended_early_at" IS '중간 마무리 시각';
COMMENT ON COLUMN "user_experiment_programs"."final_review_entry_id" IS '코스 돌아보기 기록';
COMMENT ON COLUMN "user_experiment_programs"."review_data" IS '기억에 남은 미션·도움 조건 등 구조화 회고';
COMMENT ON COLUMN "user_experiment_programs"."created_at" IS '생성 시각';
COMMENT ON COLUMN "user_experiment_programs"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "user_experiment_programs"."version" IS 'JPA 낙관적 잠금 버전';
CREATE INDEX "ix_user_experiment_programs_active" ON "user_experiment_programs" ("member_id", "status", "created_at");
CREATE UNIQUE INDEX "uq_user_experiment_programs_one_active" ON "user_experiment_programs" ("member_id") WHERE status IN ('IN_PROGRESS', 'PAUSED');
CREATE TRIGGER "trg_user_experiment_programs_updated_at" BEFORE UPDATE ON "user_experiment_programs" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "user_program_missions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_program_id" uuid NOT NULL REFERENCES "user_experiment_programs" ("id") ON DELETE CASCADE,
  "mission_id" uuid NOT NULL REFERENCES "experiment_missions" ("id") ON DELETE RESTRICT,
  "original_mission_id" uuid NOT NULL REFERENCES "experiment_missions" ("id") ON DELETE RESTRICT,
  "day_number" smallint NOT NULL,
  "scheduled_date" date,
  "status" user_mission_status NOT NULL DEFAULT 'SCHEDULED',
  "replacement_count" integer NOT NULL DEFAULT 0,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("user_program_id", "day_number"),
  CONSTRAINT "ck_user_program_missions_1" CHECK (day_number > 0),
  CONSTRAINT "ck_user_program_missions_2" CHECK (replacement_count >= 0)
);
COMMENT ON TABLE "user_program_missions" IS '교체 결과를 반영해 회원 코스에 확정된 날짜별 미션 슬롯';
COMMENT ON COLUMN "user_program_missions"."id" IS '회원 미션 슬롯 ID';
COMMENT ON COLUMN "user_program_missions"."user_program_id" IS '회원 코스 ID';
COMMENT ON COLUMN "user_program_missions"."mission_id" IS '현재 확정 미션';
COMMENT ON COLUMN "user_program_missions"."original_mission_id" IS '시작 당시 원래 미션';
COMMENT ON COLUMN "user_program_missions"."day_number" IS '코스 내 Day 번호';
COMMENT ON COLUMN "user_program_missions"."scheduled_date" IS '회원 시간대 기준 예정일';
COMMENT ON COLUMN "user_program_missions"."status" IS '미션 슬롯 상태';
COMMENT ON COLUMN "user_program_missions"."replacement_count" IS '교체 횟수';
COMMENT ON COLUMN "user_program_missions"."created_at" IS '생성 시각';
COMMENT ON COLUMN "user_program_missions"."updated_at" IS '수정 시각';
CREATE INDEX "ix_user_program_missions_schedule" ON "user_program_missions" ("user_program_id", "scheduled_date");
CREATE TRIGGER "trg_user_program_missions_updated_at" BEFORE UPDATE ON "user_program_missions" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "mission_replacement_events" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_program_mission_id" uuid NOT NULL REFERENCES "user_program_missions" ("id") ON DELETE CASCADE,
  "from_mission_id" uuid NOT NULL REFERENCES "experiment_missions" ("id") ON DELETE RESTRICT,
  "to_mission_id" uuid NOT NULL REFERENCES "experiment_missions" ("id") ON DELETE RESTRICT,
  "replacement_mode" varchar(30) NOT NULL,
  "reason" varchar(255),
  "replaced_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "mission_replacement_events" IS '시작 전·진행 중 미션 교체 이력';
COMMENT ON COLUMN "mission_replacement_events"."id" IS '교체 이벤트 ID';
COMMENT ON COLUMN "mission_replacement_events"."user_program_mission_id" IS '교체된 회원 미션 슬롯';
COMMENT ON COLUMN "mission_replacement_events"."from_mission_id" IS '교체 전 미션';
COMMENT ON COLUMN "mission_replacement_events"."to_mission_id" IS '교체 후 미션';
COMMENT ON COLUMN "mission_replacement_events"."replacement_mode" IS 'SAME_THEME, OTHER_TYPE, LIGHTER, RANDOM';
COMMENT ON COLUMN "mission_replacement_events"."reason" IS '사용자가 선택한 교체 사유';
COMMENT ON COLUMN "mission_replacement_events"."replaced_at" IS '교체 시각';
CREATE INDEX "ix_mission_replacement_events_slot" ON "mission_replacement_events" ("user_program_mission_id", "replaced_at");

CREATE TABLE "experiment_program_pauses" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_program_id" uuid NOT NULL REFERENCES "user_experiment_programs" ("id") ON DELETE CASCADE,
  "paused_from" date NOT NULL,
  "paused_until" date,
  "resumed_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT "ck_experiment_program_pauses_1" CHECK (paused_until IS NULL OR paused_until >= paused_from)
);
COMMENT ON TABLE "experiment_program_pauses" IS '오늘 하루·며칠 쉬기의 구간 이력';
COMMENT ON COLUMN "experiment_program_pauses"."id" IS '휴식 구간 ID';
COMMENT ON COLUMN "experiment_program_pauses"."user_program_id" IS '회원 코스 ID';
COMMENT ON COLUMN "experiment_program_pauses"."paused_from" IS '휴식 시작일';
COMMENT ON COLUMN "experiment_program_pauses"."paused_until" IS '계획한 휴식 종료일';
COMMENT ON COLUMN "experiment_program_pauses"."resumed_at" IS '실제 재개 시각';
COMMENT ON COLUMN "experiment_program_pauses"."created_at" IS '생성 시각';
CREATE INDEX "ix_experiment_program_pauses_program" ON "experiment_program_pauses" ("user_program_id", "paused_from");

CREATE TABLE "experiment_mission_records" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "user_program_mission_id" uuid NOT NULL UNIQUE REFERENCES "user_program_missions" ("id") ON DELETE CASCADE,
  "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
  "attempt_status" attempt_status NOT NULL,
  "emotion_intensity" smallint,
  "energy_level" smallint,
  "response_data" jsonb,
  "recorded_at" timestamptz NOT NULL DEFAULT now(),
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT "ck_experiment_mission_records_1" CHECK (emotion_intensity IS NULL OR emotion_intensity BETWEEN 1 AND 5),
  CONSTRAINT "ck_experiment_mission_records_2" CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5)
);
COMMENT ON TABLE "experiment_mission_records" IS '오늘의 작은 실험 상태와 선택형 회고; 원문·태그는 연결 entry에 저장';
COMMENT ON COLUMN "experiment_mission_records"."id" IS '미션 기록 ID';
COMMENT ON COLUMN "experiment_mission_records"."user_program_mission_id" IS '대상 회원 미션 슬롯';
COMMENT ON COLUMN "experiment_mission_records"."entry_id" IS 'EXPERIMENT_MISSION 기록 봉투';
COMMENT ON COLUMN "experiment_mission_records"."attempt_status" IS '해봄·조금 해봄·쉼·다르게 시도 등';
COMMENT ON COLUMN "experiment_mission_records"."emotion_intensity" IS '당시 감정 강도 1~5';
COMMENT ON COLUMN "experiment_mission_records"."energy_level" IS '당시 에너지 수준 1~5';
COMMENT ON COLUMN "experiment_mission_records"."response_data" IS '미션별 선택형 질문 답변';
COMMENT ON COLUMN "experiment_mission_records"."recorded_at" IS '기록 완료 시각';
COMMENT ON COLUMN "experiment_mission_records"."created_at" IS '생성 시각';
COMMENT ON COLUMN "experiment_mission_records"."updated_at" IS '수정 시각';
CREATE TRIGGER "trg_experiment_mission_records_updated_at" BEFORE UPDATE ON "experiment_mission_records" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "experiment_recommendations" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "program_id" uuid NOT NULL REFERENCES "experiment_programs" ("id") ON DELETE RESTRICT,
  "source" recommendation_source NOT NULL,
  "source_entry_id" uuid REFERENCES "entries" ("id") ON DELETE SET NULL,
  "ai_reflection_id" uuid REFERENCES "ai_reflections" ("id") ON DELETE SET NULL,
  "reason_text" text,
  "status" recommendation_status NOT NULL DEFAULT 'SUGGESTED',
  "accepted_user_program_id" uuid UNIQUE REFERENCES "user_experiment_programs" ("id") ON DELETE SET NULL,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "decided_at" timestamptz,
  "expires_at" timestamptz
);
COMMENT ON TABLE "experiment_recommendations" IS '자동 시작하지 않는 선택형 작은 실험 코스 제안';
COMMENT ON COLUMN "experiment_recommendations"."id" IS '추천 ID';
COMMENT ON COLUMN "experiment_recommendations"."member_id" IS '추천 대상 회원';
COMMENT ON COLUMN "experiment_recommendations"."program_id" IS '추천 코스';
COMMENT ON COLUMN "experiment_recommendations"."source" IS '기본/기록/체크인/주간/랜덤/AI 추천';
COMMENT ON COLUMN "experiment_recommendations"."source_entry_id" IS '추천 근거 기록; 체크인·주간 회고도 entry로 연결';
COMMENT ON COLUMN "experiment_recommendations"."ai_reflection_id" IS '추천을 만든 AI 정리';
COMMENT ON COLUMN "experiment_recommendations"."reason_text" IS '단정하지 않는 사용자 노출 추천 근거';
COMMENT ON COLUMN "experiment_recommendations"."status" IS '추천 선택 상태';
COMMENT ON COLUMN "experiment_recommendations"."accepted_user_program_id" IS '수락 후 생성된 회원 코스';
COMMENT ON COLUMN "experiment_recommendations"."created_at" IS '추천 시각';
COMMENT ON COLUMN "experiment_recommendations"."decided_at" IS '수락/거절 시각';
COMMENT ON COLUMN "experiment_recommendations"."expires_at" IS '추천 만료 시각';
CREATE INDEX "ix_experiment_recommendations_member" ON "experiment_recommendations" ("member_id", "status", "created_at");

-- P1/P2 tables intentionally not created in Beta 1
-- P1: social_identity_link_events - 재인증 기반 계정 연결·병합·복구 감사 이력
-- P1: member_quote_preferences - 오늘의 문장 표시 여부·빈도·선호/숨김 주제
-- P1: tag_aliases - 태그 동의어·병합 대상과 대표 태그 매핑
-- P1: ai_insight_segments - AI 문장별 동의·수정·보류와 근거 기록 연결
-- P1: monthly_reflections - 월간 흐름·패턴·사용자 자기정리와 근거 스냅샷
-- P1: data_export_jobs - 기록 다운로드·내보내기 요청 및 만료 파일 관리
-- P1: experiment_badges - 비경쟁형 진행 배지 정의와 회원 수여 이력
-- P2: entry_attachments - 사진·음성 기록 메타데이터와 보관 객체 참조
-- P2: anonymous_shares - 사용자가 선택한 기록 일부의 익명 공유 스냅샷
-- P2: share_reactions - 좋아요 대신 공감 중심의 조용한 반응
-- P2: wearable_connections - Watch·Wear OS·헬스 데이터 연결 동의와 토큰 참조
-- P2: external_calendar_connections - 외부 캘린더 연동 동의와 동기화 상태
-- P2: partner_program_sources - 전문가·콘텐츠 파트너 코스 출처와 검수 이력
