-- 오늘의 문장 관리자 편집(관리자 웹 5단계, Admin Web Implementation Spec §10.4). record_prompts(V24)와 같은
-- 패턴: 발행본은 절대 UPDATE하지 않고 같은 code의 새 row(version_no 증가)로만 바꾼다. PUBLISHED 문장을
-- 수정해 저장하면 서버가 자동으로 다음 버전을 만들고, 새 버전을 게시하기 전까지 앱은 기존 문장을 계속
-- 노출한다(§10.3). member_saved_quotes/entries.quote_id가 이미 quotes row를 참조하므로(ON DELETE
-- CASCADE/SET NULL) hard delete를 하지 않는 한 저장 기록·과거 기록과의 연결은 그대로 유지된다.

-- uk_quotes_text(V4)는 시드 스크립트의 upsert 편의용 제약이라 애플리케이션 코드가 의존하지 않는다.
-- code+version_no 모델에서는 문장 복제·리비전마다 본문이 같거나 비슷할 수 있어 이 제약이 오히려
-- 정상적인 편집 흐름을 막는다.
ALTER TABLE "quotes" DROP CONSTRAINT "uk_quotes_text";

ALTER TABLE "quotes"
  ADD COLUMN "code" varchar(80),
  ADD COLUMN "version_no" int,
  ADD COLUMN "active_from" timestamptz,
  ADD COLUMN "active_until" timestamptz,
  ADD COLUMN "supersedes_quote_id" uuid REFERENCES "quotes" ("id"),
  ADD COLUMN "created_by_admin_id" uuid REFERENCES "admin_users" ("id"),
  ADD COLUMN "version" bigint NOT NULL DEFAULT 0;

-- 기존 문장에는 안정적인 code(자기 자신의 id)와 version_no 1을 부여한다(§10.4).
UPDATE "quotes" SET "code" = "id"::text, "version_no" = 1 WHERE "code" IS NULL;

ALTER TABLE "quotes"
  ALTER COLUMN "code" SET NOT NULL,
  ALTER COLUMN "version_no" SET NOT NULL,
  ADD CONSTRAINT "uk_quotes_code_version_no" UNIQUE ("code", "version_no"),
  ADD CONSTRAINT "ck_quotes_active_period" CHECK (
    "active_from" IS NULL OR "active_until" IS NULL OR "active_from" < "active_until");

COMMENT ON COLUMN "quotes"."code" IS '문장을 식별하는 안정 키 - 여러 버전이 이 code를 공유';
COMMENT ON COLUMN "quotes"."version_no" IS '같은 code 안에서의 버전 번호, 1부터 증가';
COMMENT ON COLUMN "quotes"."active_from" IS '노출 시작 시각; NULL이면 발행 즉시 노출';
COMMENT ON COLUMN "quotes"."active_until" IS '노출 종료 시각; NULL이면 계속 노출';
COMMENT ON COLUMN "quotes"."supersedes_quote_id" IS '이 버전이 대체한 이전 버전';
COMMENT ON COLUMN "quotes"."created_by_admin_id" IS '이 버전을 만든 관리자; 초기 시드 데이터는 NULL';
COMMENT ON COLUMN "quotes"."version" IS 'JPA 낙관적 잠금 버전(콘텐츠 버전인 version_no와는 별개)';

-- 같은 code에서 PUBLISHED는 동시에 하나만 허용한다(record_prompts와 동일한 규칙).
CREATE UNIQUE INDEX "ix_quotes_code_published" ON "quotes" ("code") WHERE "status" = 'PUBLISHED';
