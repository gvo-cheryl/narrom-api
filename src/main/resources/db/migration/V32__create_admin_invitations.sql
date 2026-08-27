CREATE TABLE "admin_invitations" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
	"email" varchar(320) NOT NULL,
	"invited_by_admin_id" uuid REFERENCES "admin_users" ("id"),
	"invited_at" timestamptz NOT NULL DEFAULT now(),
	"consumed_at" timestamptz,
	"consumed_admin_user_id" uuid REFERENCES "admin_users" ("id"),
	"revoked_at" timestamptz
);
COMMENT ON TABLE "admin_invitations" IS '로그인 전에 미리 등록해두는 관리자 초대 - 이메일이 일치하는 첫 Google 로그인 시 admin_users로 전환된다(AdminLoginResolver). invited_by_admin_id가 NULL이면 SUPER_ADMIN 자동 시드(AdminSuperAdminInvitationSeeder)로 생성된 것이다.';
COMMENT ON COLUMN "admin_invitations"."consumed_at" IS '초대가 실제 로그인으로 전환된 시각; NULL이면 아직 대기 중';
COMMENT ON COLUMN "admin_invitations"."revoked_at" IS '전환되기 전에 취소된 시각; consumed_at과 동시에 있을 수 없다(애플리케이션이 보장)';

-- 대기 중(아직 consumed도 revoked도 아닌) 같은 이메일 초대는 하나만 허용한다.
CREATE UNIQUE INDEX "ux_admin_invitations_pending_email" ON "admin_invitations" (lower(email))
	WHERE consumed_at IS NULL AND revoked_at IS NULL;

CREATE TABLE "admin_invitation_roles" (
	"admin_invitation_id" uuid NOT NULL REFERENCES "admin_invitations" ("id") ON DELETE CASCADE,
	"role" admin_role NOT NULL,
	PRIMARY KEY ("admin_invitation_id", "role")
);
COMMENT ON TABLE "admin_invitation_roles" IS '초대에 부여될 역할 - 전환 시 그대로 admin_user_roles로 복사된다';
