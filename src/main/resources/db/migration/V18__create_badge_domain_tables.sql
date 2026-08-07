CREATE TYPE "badge_category" AS ENUM ('TRIAL', 'DISCOVERY', 'RETURN', 'SELF_ORGANIZATION');
CREATE TYPE "badge_code" AS ENUM (
  'FIRST_ENTRY',
  'FIRST_CHECKIN',
  'FIRST_EXPERIMENT_START',
  'FIRST_WEEKLY_REFLECTION',
  'FIRST_EXPERIMENT_REVIEW',
  'FIRST_PERSONAL_SUMMARY',
  'RETURN_AFTER_GAP',
  'RESUME_PAUSED_EXPERIMENT',
  'FIRST_SELF_REFLECTION',
  'SELF_REFLECTION_5'
);

CREATE TABLE "badge_definitions" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" badge_code NOT NULL,
  "category" badge_category NOT NULL,
  "title" varchar(80) NOT NULL,
  "description" varchar(200) NOT NULL,
  "display_order" smallint NOT NULL,
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("code")
);
COMMENT ON TABLE "badge_definitions" IS '비경쟁형 뱃지 카탈로그(Beta 1은 애플리케이션 시드 데이터로만 채움, 관리 UI 없음)';
COMMENT ON COLUMN "badge_definitions"."code" IS '뱃지 판정 로직이 참조하는 안정적인 코드';
COMMENT ON COLUMN "badge_definitions"."category" IS '시도/발견/복귀/자기정리 4종';
COMMENT ON COLUMN "badge_definitions"."display_order" IS '나의 뱃지함 화면 노출 순서';
CREATE TRIGGER "trg_badge_definitions_updated_at" BEFORE UPDATE ON "badge_definitions" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "member_badges" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
  "badge_definition_id" uuid NOT NULL REFERENCES "badge_definitions" ("id"),
  "earned_at" timestamptz NOT NULL DEFAULT now(),
  "created_at" timestamptz NOT NULL DEFAULT now(),
  UNIQUE ("member_id", "badge_definition_id")
);
COMMENT ON TABLE "member_badges" IS '회원이 실제로 획득한 뱃지. 같은 뱃지는 회원당 한 번만 획득한다(재획득 없음)';
COMMENT ON COLUMN "member_badges"."earned_at" IS '획득 시각';
CREATE INDEX "idx_member_badges_member" ON "member_badges" ("member_id");
