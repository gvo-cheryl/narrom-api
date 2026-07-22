-- Naroom Beta 1 - Account domain initial schema
-- Scope: Account domain only (6 tables, 5 enums).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE "member_status" AS ENUM ('ACTIVE', 'LOCKED', 'PENDING_DELETION');
CREATE TYPE "social_provider" AS ENUM ('KAKAO', 'GOOGLE');
CREATE TYPE "identity_status" AS ENUM ('ACTIVE', 'REVOKED');
CREATE TYPE "consent_type" AS ENUM ('TERMS', 'PRIVACY', 'AI_PROCESSING');
CREATE TYPE "notification_type" AS ENUM ('WEEKLY_REFLECTION', 'EXPERIMENT_MISSION', 'DAILY_QUOTE');

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
