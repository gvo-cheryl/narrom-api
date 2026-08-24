-- 기타 앱 문구 관리(관리자 웹 5단계, Admin Web Implementation Spec §11). record_prompts(V24)/quotes(V25)와
-- 같은 code+version_no 버전 관리 패턴: 발행본은 절대 UPDATE하지 않고 같은 content_key(+locale)의 새 row로만
-- 바꾼다. §11.1의 CMS화 기준(운영 중 조정 가능성이 높고, 바뀌어도 화면 구조·로직이 변하지 않고, 서버 응답이
-- 없어도 앱 fallback으로 동작 가능한 문구)을 통과한 것만 이 테이블에 등록한다 - 버튼 동작·route·enum·
-- validation 규칙은 콘텐츠로 옮기지 않는다(§11.1).

CREATE TYPE "app_content_value_type" AS ENUM ('TEXT', 'JSON');
CREATE TYPE "app_content_item_status" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

CREATE TABLE "app_content_items" (
  "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  "content_key" varchar(120) NOT NULL,
  "surface" varchar(60) NOT NULL,
  "locale" varchar(10) NOT NULL DEFAULT 'ko-KR',
  "value_type" "app_content_value_type" NOT NULL,
  "value_text" text,
  "value_json" jsonb,
  "schema_version" varchar(20) NOT NULL,
  "version_no" int NOT NULL,
  "status" "app_content_item_status" NOT NULL DEFAULT 'DRAFT',
  "active_from" timestamptz,
  "active_until" timestamptz,
  "fallback_required" boolean NOT NULL DEFAULT true,
  "created_by_admin_id" uuid NOT NULL REFERENCES "admin_users" ("id"),
  "supersedes_item_id" uuid REFERENCES "app_content_items" ("id"),
  "created_at" timestamptz NOT NULL DEFAULT now(),
  "updated_at" timestamptz NOT NULL DEFAULT now(),
  "version" bigint NOT NULL DEFAULT 0,
  UNIQUE ("content_key", "locale", "version_no"),
  CHECK ("active_from" IS NULL OR "active_until" IS NULL OR "active_from" < "active_until"),
  -- TEXT 항목은 value_text만, JSON 항목(구조화된 문장 묶음)은 value_json만 채운다(§11.6).
  CHECK (
    ("value_type" = 'TEXT' AND "value_text" IS NOT NULL AND "value_json" IS NULL) OR
    ("value_type" = 'JSON' AND "value_json" IS NOT NULL AND "value_text" IS NULL)
  )
);
COMMENT ON TABLE "app_content_items" IS '기타 앱 문구(홈 인사말, 안내, placeholder, 빈 상태 등) - content_key+locale+version_no로 버전 관리, 발행본은 새 row로만 교체(UPDATE 금지)';
COMMENT ON COLUMN "app_content_items"."id" IS '문구 버전 ID';
COMMENT ON COLUMN "app_content_items"."content_key" IS '문구를 식별하는 안정 키(예: home.greeting.default) - 여러 버전과 로케일이 이 키를 공유';
COMMENT ON COLUMN "app_content_items"."surface" IS '문구가 쓰이는 화면/영역 구분(관리자 목록 탭 필터용, 예: home/checkin/record/experiment)';
COMMENT ON COLUMN "app_content_items"."locale" IS 'BCP 47 로케일 태그. Beta 1은 ko-KR만 운영하지만 컬럼은 다국어 확장을 대비해 둔다';
COMMENT ON COLUMN "app_content_items"."value_type" IS '단순 문자열(TEXT)인지 구조화된 문장 묶음(JSON)인지 구분';
COMMENT ON COLUMN "app_content_items"."value_text" IS 'value_type=TEXT일 때의 문구 본문';
COMMENT ON COLUMN "app_content_items"."value_json" IS 'value_type=JSON일 때의 구조화된 문구 데이터(schema_version이 그 구조를 식별)';
COMMENT ON COLUMN "app_content_items"."schema_version" IS 'value_json 구조의 버전 식별자. 앱이 지원하지 않는 schema_version을 받으면 자체 fallback을 사용한다(§11.4)';
COMMENT ON COLUMN "app_content_items"."version_no" IS '같은 content_key+locale 안에서의 버전 번호, 1부터 증가';
COMMENT ON COLUMN "app_content_items"."status" IS 'DRAFT/PUBLISHED/ARCHIVED - 같은 content_key+locale에서 PUBLISHED는 동시에 하나만 허용(ix_app_content_items_published)';
COMMENT ON COLUMN "app_content_items"."active_from" IS '노출 시작 시각; NULL이면 발행 즉시 노출';
COMMENT ON COLUMN "app_content_items"."active_until" IS '노출 종료 시각; NULL이면 계속 노출';
COMMENT ON COLUMN "app_content_items"."fallback_required" IS '앱이 이 키에 대해 자체 fallback 문구를 반드시 갖고 있어야 하는지 표시(관리자 화면에서 fallback 등록 여부 점검용)';
COMMENT ON COLUMN "app_content_items"."created_by_admin_id" IS '이 버전을 만든 관리자';
COMMENT ON COLUMN "app_content_items"."supersedes_item_id" IS '이 버전이 대체한 이전 버전';
COMMENT ON COLUMN "app_content_items"."version" IS 'JPA 낙관적 잠금 버전(콘텐츠 버전인 version_no와는 별개)';
CREATE UNIQUE INDEX "ix_app_content_items_published" ON "app_content_items" ("content_key", "locale") WHERE "status" = 'PUBLISHED';
CREATE INDEX "ix_app_content_items_surface_listing" ON "app_content_items" ("surface", "content_key");
CREATE TRIGGER "trg_app_content_items_updated_at" BEFORE UPDATE ON "app_content_items" FOR EACH ROW EXECUTE FUNCTION set_updated_at();
