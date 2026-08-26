-- 14.6절: "코드 또는 관리 파일에서 버전 관리"라는 정책 자체는 유지하되, 관리 파일 역할을 관리자 웹의
-- 버전 관리 화면으로 옮긴다. 기존에 AiPromptVersionResolver.getOrCreateCommon/Feature가 자동 생성하던
-- 북마킹용 row(content가 없는 row)와, 이번에 추가하는 관리자 작성 콘텐츠(content가 있는 row)를 같은
-- 테이블에서 구분한다 - content IS NOT NULL 여부로 완전히 분리되어 서로 충돌하지 않는다.

CREATE TYPE "ai_prompt_version_status" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

ALTER TABLE "ai_prompt_versions"
    ADD COLUMN "content" text,
    ADD COLUMN "model_name" varchar(80),
    ADD COLUMN "output_max_length" integer,
    ADD COLUMN "status" ai_prompt_version_status,
    ADD COLUMN "supersedes_version_id" uuid REFERENCES "ai_prompt_versions" ("id"),
    ADD COLUMN "created_by_admin_id" uuid;

UPDATE "ai_prompt_versions" SET "status" = CASE WHEN "is_active" THEN 'PUBLISHED' ELSE 'ARCHIVED' END::ai_prompt_version_status;

ALTER TABLE "ai_prompt_versions" ALTER COLUMN "status" SET NOT NULL;
ALTER TABLE "ai_prompt_versions" ALTER COLUMN "status" SET DEFAULT 'PUBLISHED';
ALTER TABLE "ai_prompt_versions" DROP COLUMN "is_active";

COMMENT ON COLUMN "ai_prompt_versions"."content" IS '관리자가 작성한 실제 지침 본문. NULL이면 코드(AiInstructionCatalog)가 만든 북마킹용 row.';
COMMENT ON COLUMN "ai_prompt_versions"."model_name" IS 'FEATURE 범위에서만 의미 있음. NULL이면 naroom.ai.openai.model 기본값을 쓴다.';
COMMENT ON COLUMN "ai_prompt_versions"."output_max_length" IS 'FEATURE 범위에서만 의미 있음. summary 필드 최대 글자 수, NULL이면 제한 없음.';
COMMENT ON COLUMN "ai_prompt_versions"."status" IS 'DRAFT/PUBLISHED/ARCHIVED. content가 NULL인 코드 북마킹 row는 항상 PUBLISHED로 둔다.';
COMMENT ON COLUMN "ai_prompt_versions"."supersedes_version_id" IS '리비전 이력 추적용 - 어떤 발행본에서 새 초안을 만들었는지.';
COMMENT ON COLUMN "ai_prompt_versions"."created_by_admin_id" IS '관리자가 작성한 row에서만 값이 있다. admin_users는 별도 신원 체계라 FK 대신 원문 UUID만 보관한다.';
