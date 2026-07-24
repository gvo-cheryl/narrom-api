-- Naroom Beta 1
-- V2에서 BIGINT 기반으로 만든 Content 4개 테이블을
-- reference 스키마(naroom_beta1_full_schema_reference.sql) 기준 UUID 스키마로 재생성한다.
-- V2는 이미 적용된 마이그레이션이라 직접 수정하지 않는다.
-- entries.quote_id는 Record 도메인 구현 시 다시 정리한다.

ALTER TABLE "entries" DROP CONSTRAINT "fk_entries_quote";

DROP TABLE "quote_topic_links";
DROP TABLE "member_saved_quotes";
DROP TABLE "quotes";
DROP TABLE "quote_topics";

-- ---------------------------------------------------------------------------
-- Content
-- ---------------------------------------------------------------------------

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
