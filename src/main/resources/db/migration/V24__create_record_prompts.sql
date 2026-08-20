-- 기록 시작 질문 마스터(관리자 웹 5단계, Admin Web Implementation Spec §9.2). entries.prompt_snapshot(V5)이
-- 기록 당시 화면에 보인 질문 원문을 이미 스냅샷으로 남기므로, 여기 내용을 새 버전으로 바꿔도 과거 기록의
-- 의미는 바뀌지 않는다 - 그래서 수정은 UPDATE가 아니라 같은 code의 새 row(version_no 증가)로만 한다.

CREATE TYPE "record_prompt_status" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

CREATE TABLE "record_prompts" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "code" varchar(80) NOT NULL,
  "version_no" int NOT NULL,
  "question_text" text NOT NULL,
  "helper_text" text,
  "entry_type" entry_type NOT NULL DEFAULT 'PROMPT',
  "display_order" int NOT NULL,
  "status" record_prompt_status NOT NULL DEFAULT 'DRAFT',
  "active_from" timestamptz,
  "active_until" timestamptz,
  "supersedes_prompt_id" uuid REFERENCES "record_prompts" ("id"),
  "created_by_admin_id" uuid NOT NULL REFERENCES "admin_users" ("id"),
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  UNIQUE ("code", "version_no"),
  CHECK ("active_from" IS NULL OR "active_until" IS NULL OR "active_from" < "active_until")
);
COMMENT ON TABLE "record_prompts" IS '기록 시작 질문 마스터 - code+version_no로 버전 관리, 발행본은 새 row로만 교체(UPDATE 금지)';
COMMENT ON COLUMN "record_prompts"."id" IS '질문 버전 ID';
COMMENT ON COLUMN "record_prompts"."code" IS '질문을 식별하는 안정 키 - 여러 버전이 이 code를 공유';
COMMENT ON COLUMN "record_prompts"."version_no" IS '같은 code 안에서의 버전 번호, 1부터 증가';
COMMENT ON COLUMN "record_prompts"."question_text" IS '질문 본문';
COMMENT ON COLUMN "record_prompts"."helper_text" IS '질문 아래 보조 설명';
COMMENT ON COLUMN "record_prompts"."entry_type" IS '이 질문으로 시작하는 기록의 entries.entry_type 기본값';
COMMENT ON COLUMN "record_prompts"."display_order" IS '같은 상태(PUBLISHED) 질문 사이의 노출 순서';
COMMENT ON COLUMN "record_prompts"."status" IS 'DRAFT/PUBLISHED/ARCHIVED - 같은 code에서 PUBLISHED는 동시에 하나만 허용(ix_record_prompts_published)';
COMMENT ON COLUMN "record_prompts"."active_from" IS '노출 시작 시각; NULL이면 발행 즉시 노출';
COMMENT ON COLUMN "record_prompts"."active_until" IS '노출 종료 시각; NULL이면 계속 노출';
COMMENT ON COLUMN "record_prompts"."supersedes_prompt_id" IS '이 버전이 대체한 이전 버전';
COMMENT ON COLUMN "record_prompts"."created_by_admin_id" IS '이 버전을 만든 관리자';
COMMENT ON COLUMN "record_prompts"."created_at" IS '생성 시각';
COMMENT ON COLUMN "record_prompts"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "record_prompts"."version" IS 'JPA 낙관적 잠금 버전(콘텐츠 버전인 version_no와는 별개)';
CREATE UNIQUE INDEX "ix_record_prompts_published" ON "record_prompts" ("code") WHERE "status" = 'PUBLISHED';
CREATE INDEX "ix_record_prompts_active_listing" ON "record_prompts" ("status", "display_order") WHERE "status" = 'PUBLISHED';
CREATE TRIGGER "trg_record_prompts_updated_at" BEFORE UPDATE ON "record_prompts" FOR EACH ROW EXECUTE FUNCTION set_updated_at();
