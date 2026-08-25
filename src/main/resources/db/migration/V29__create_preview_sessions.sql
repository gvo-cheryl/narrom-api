-- 모바일 미리보기 세션(관리자 웹 Epic D, Admin Web Implementation Spec §16.2). naroom-admin이 iframe으로
-- 띄우는 naroom-app 웹 preview 빌드가 이 세션의 토큰으로 지정된 DRAFT 콘텐츠 버전만 조회할 수 있다.
-- 회원 auth_sessions/admin_sessions와 완전히 분리된 별도 체계 - 원문 토큰은 저장하지 않고 해시만 둔다.

CREATE TABLE "preview_sessions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "admin_user_id" uuid NOT NULL REFERENCES "admin_users" ("id"),
  "token_hash" varchar(128) NOT NULL UNIQUE,
  "selected_content_versions" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "scenario_key" varchar(60),
  "expires_at" timestamptz NOT NULL,
  "revoked_at" timestamptz,
  "created_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "preview_sessions" IS '관리자 미리보기 세션 - 짧은 수명 preview token으로 지정 DRAFT 버전만 노출';
COMMENT ON COLUMN "preview_sessions"."admin_user_id" IS '이 미리보기 세션을 발급한 관리자';
COMMENT ON COLUMN "preview_sessions"."token_hash" IS 'preview token의 SHA-256 해시 - 원문은 저장하지 않는다';
COMMENT ON COLUMN "preview_sessions"."selected_content_versions" IS '콘텐츠 종류별로 미리볼 DRAFT 버전 id를 담은 맵(예: {"quote": "<uuid>"})';
COMMENT ON COLUMN "preview_sessions"."scenario_key" IS '홈 화면 등에서 재현할 합성 시나리오 식별자(§16.3), 지정하지 않으면 NULL';
COMMENT ON COLUMN "preview_sessions"."expires_at" IS '발급 시각 기준 15~30분 뒤로 설정 - naroom.admin.preview.token-timeout';
COMMENT ON COLUMN "preview_sessions"."revoked_at" IS '수동 폐기 시각; NULL이면 만료 전까지 유효';
CREATE INDEX "ix_preview_sessions_admin_user" ON "preview_sessions" ("admin_user_id");
