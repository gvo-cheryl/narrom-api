-- Naroom Beta 1
-- Check-in 2개 테이블(check_ins, check_in_emotions)을
-- reference 스키마(naroom_beta1_full_schema_reference.sql) 기준으로 생성한다.
-- entries/tags는 Record 도메인(V5)에서 이미 reference 기준으로 만들어져 있다.

CREATE TABLE "check_ins" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "entry_id" uuid NOT NULL UNIQUE REFERENCES "entries" ("id") ON DELETE CASCADE,
    "check_in_date" date NOT NULL,
    "emotion_intensity" smallint,
    "energy_level" smallint,
    "memorable_event" text,
    "gratitude_note" text,
    "current_need" text,
    "free_note" text,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    "version" bigint NOT NULL DEFAULT 0,
    UNIQUE ("member_id", "check_in_date"),
    CONSTRAINT "ck_check_ins_1" CHECK (emotion_intensity IS NULL OR emotion_intensity BETWEEN 1 AND 5),
    CONSTRAINT "ck_check_ins_2" CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5)
);
COMMENT ON TABLE "check_ins" IS '하루 1회 기준의 선택형 감정·에너지 체크인';
COMMENT ON COLUMN "check_ins"."id" IS '체크인 ID';
COMMENT ON COLUMN "check_ins"."member_id" IS '체크인 회원';
COMMENT ON COLUMN "check_ins"."entry_id" IS 'LifeTime에 표시할 CHECK_IN 기록 봉투';
COMMENT ON COLUMN "check_ins"."check_in_date" IS '회원 시간대 기준 체크인 날짜';
COMMENT ON COLUMN "check_ins"."emotion_intensity" IS '감정 강도 1~5';
COMMENT ON COLUMN "check_ins"."energy_level" IS '에너지 수준 1~5';
COMMENT ON COLUMN "check_ins"."memorable_event" IS '오늘 마음에 남은 일';
COMMENT ON COLUMN "check_ins"."gratitude_note" IS '감사하거나 다행이었던 일';
COMMENT ON COLUMN "check_ins"."current_need" IS '지금 필요하다고 느끼는 것';
COMMENT ON COLUMN "check_ins"."free_note" IS '추가 자유 기록';
COMMENT ON COLUMN "check_ins"."created_at" IS '생성 시각';
COMMENT ON COLUMN "check_ins"."updated_at" IS '수정 시각';
COMMENT ON COLUMN "check_ins"."version" IS 'JPA 낙관적 잠금 버전';
CREATE TRIGGER "trg_check_ins_updated_at" BEFORE UPDATE ON "check_ins" FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE "check_in_emotions" (
    "check_in_id" uuid REFERENCES "check_ins" ("id") ON DELETE CASCADE,
    "tag_id" uuid REFERENCES "tags" ("id") ON DELETE RESTRICT,
    "selected_at" timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY ("check_in_id", "tag_id")
);
COMMENT ON TABLE "check_in_emotions" IS '체크인에서 사용자가 직접 선택한 복수 감정';
COMMENT ON COLUMN "check_in_emotions"."check_in_id" IS '체크인 ID';
COMMENT ON COLUMN "check_in_emotions"."tag_id" IS 'EMOTION 분류 태그 ID';
COMMENT ON COLUMN "check_in_emotions"."selected_at" IS '선택 시각';
CREATE INDEX "ix_check_in_emotions_tag" ON "check_in_emotions" ("tag_id", "check_in_id");
