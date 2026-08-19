-- 관리자 웹 기반: Google OIDC 인증·역할·세션·감사 로그. 회원 도메인(members 등)과 완전히 분리된
-- 별도 신원 체계다 - 회원 테이블을 조회해 관리자 권한으로 승격하지 않는다.

CREATE TYPE "admin_status" AS ENUM ('ACTIVE', 'DISABLED');
CREATE TYPE "admin_role" AS ENUM ('SUPER_ADMIN', 'CONTENT_EDITOR', 'AI_OPERATOR', 'SUPPORT_READ_ONLY');

CREATE TABLE "admin_users" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "google_sub" varchar(255) NOT NULL,
  "email" varchar(320) NOT NULL,
  "email_verified" boolean NOT NULL DEFAULT false,
  "display_name" varchar(100),
  "status" admin_status NOT NULL DEFAULT 'ACTIVE',
  "approved_by_admin_id" uuid REFERENCES "admin_users" ("id"),
  "approved_at" timestamptz,
  "last_login_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  UNIQUE ("google_sub")
);
COMMENT ON TABLE "admin_users" IS '관리자 웹 신원 - 회원(members)과 별개의 신원 체계';
COMMENT ON COLUMN "admin_users"."id" IS '관리자 ID';
COMMENT ON COLUMN "admin_users"."google_sub" IS 'Google OIDC 불변 사용자 식별자 - 인가 키(이메일 아님)';
COMMENT ON COLUMN "admin_users"."email" IS '표시·변경 감지용 메타데이터. 인가 키로 쓰지 않는다';
COMMENT ON COLUMN "admin_users"."email_verified" IS 'Google이 검증한 이메일인지 여부';
COMMENT ON COLUMN "admin_users"."display_name" IS '관리자 화면 표시 이름';
COMMENT ON COLUMN "admin_users"."status" IS 'DISABLED면 기존 세션도 다음 요청에서 거부';
COMMENT ON COLUMN "admin_users"."approved_by_admin_id" IS '승인한 관리자; bootstrap으로 생성된 최초 관리자는 NULL';
COMMENT ON COLUMN "admin_users"."approved_at" IS '승인 시각';
COMMENT ON COLUMN "admin_users"."last_login_at" IS '마지막 로그인 시각';
COMMENT ON COLUMN "admin_users"."created_at" IS '생성 시각';
COMMENT ON COLUMN "admin_users"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "admin_users"."version" IS 'JPA 낙관적 잠금 버전';
CREATE TRIGGER "trg_admin_users_updated_at" BEFORE UPDATE ON "admin_users" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "admin_user_roles" (
  "admin_user_id" uuid NOT NULL REFERENCES "admin_users" ("id") ON DELETE CASCADE,
  "role" admin_role NOT NULL,
  PRIMARY KEY ("admin_user_id", "role")
);
COMMENT ON TABLE "admin_user_roles" IS '관리자별 부여된 역할 - 한 관리자가 여러 역할을 가질 수 있다';

CREATE TABLE "admin_sessions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "admin_user_id" uuid NOT NULL REFERENCES "admin_users" ("id") ON DELETE CASCADE,
  "session_token_hash" varchar(128) NOT NULL,
  "issued_at" timestamptz NOT NULL DEFAULT now(),
  "last_used_at" timestamptz NOT NULL DEFAULT now(),
  "expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz,
  "ip_hash" varchar(128),
  "user_agent_summary" varchar(255),
  UNIQUE ("session_token_hash")
);
COMMENT ON TABLE "admin_sessions" IS '원문 세션 토큰을 저장하지 않는 관리자 세션. 회원 auth_sessions와 완전히 분리된 쿠키·체계';
COMMENT ON COLUMN "admin_sessions"."session_token_hash" IS '세션 토큰 단방향 해시';
COMMENT ON COLUMN "admin_sessions"."expires_at" IS 'absolute timeout(권장 8시간) 기준 만료 시각';
COMMENT ON COLUMN "admin_sessions"."last_used_at" IS 'idle timeout(권장 30분) 판정 기준';
COMMENT ON COLUMN "admin_sessions"."revoked_at" IS '로그아웃·관리자 비활성화 등으로 폐기된 시각';
COMMENT ON COLUMN "admin_sessions"."ip_hash" IS '보안 목적 최소 메타데이터; 원문 IP 미저장';
COMMENT ON COLUMN "admin_sessions"."user_agent_summary" IS '보안 목적 최소 메타데이터; User-Agent 원문 미저장';
CREATE INDEX "ix_admin_sessions_admin_user_active" ON "admin_sessions" ("admin_user_id", "expires_at") WHERE revoked_at IS NULL;

CREATE TABLE "admin_audit_logs" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "actor_admin_id" uuid REFERENCES "admin_users" ("id"),
  "action" varchar(100) NOT NULL,
  "resource_type" varchar(100) NOT NULL,
  "resource_id" varchar(255),
  "before_data" jsonb,
  "after_data" jsonb,
  "change_reason" text,
  "trace_id" varchar(100),
  "request_method" varchar(10),
  "request_path" varchar(500),
  "outcome" varchar(10) NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "admin_audit_logs" IS 'append-only 감사 로그. 비밀키·토큰·쿠키·기록 원문·AI 입출력 원문은 저장하지 않는다';
COMMENT ON COLUMN "admin_audit_logs"."actor_admin_id" IS '행위자; 인증 실패처럼 신원이 확정되지 않은 경우 NULL';
COMMENT ON COLUMN "admin_audit_logs"."outcome" IS 'SUCCESS 또는 FAILURE';
CREATE INDEX "ix_admin_audit_logs_actor" ON "admin_audit_logs" ("actor_admin_id", "created_at");
CREATE INDEX "ix_admin_audit_logs_resource" ON "admin_audit_logs" ("resource_type", "resource_id");
