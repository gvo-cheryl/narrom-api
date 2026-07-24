-- Naroom Beta 1
-- 체크인 감정 선택 화면이 "편안한 결/기운이 도는/가라앉는/조여드는/뜨거워지는/이름 붙이기 어려운"
-- 6개 카테고리로 감정 태그를 묶어 보여주기 위한 그룹 메타데이터.
-- tags 테이블 자체는 건드리지 않고 순수 참조 테이블만 추가한다(V6에서 이미 넣은 48개 시스템 감정 태그는 그대로 유지).

CREATE TABLE "emotion_tag_groups" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "code" varchar(50) NOT NULL UNIQUE,
    "name" varchar(80) NOT NULL,
    "display_order" integer NOT NULL DEFAULT 0,
    CONSTRAINT "ck_emotion_tag_groups_display_order" CHECK (display_order >= 0)
);
COMMENT ON TABLE "emotion_tag_groups" IS '체크인 감정 선택 화면에 쓰이는 감정 태그 표시 카테고리';
COMMENT ON COLUMN "emotion_tag_groups"."id" IS '그룹 ID';
COMMENT ON COLUMN "emotion_tag_groups"."code" IS '변하지 않는 그룹 코드';
COMMENT ON COLUMN "emotion_tag_groups"."name" IS '사용자 표시 그룹명';
COMMENT ON COLUMN "emotion_tag_groups"."display_order" IS '화면 표시 순서';

CREATE TABLE "emotion_tag_group_members" (
    "tag_id" uuid PRIMARY KEY REFERENCES "tags" ("id") ON DELETE CASCADE,
    "group_id" uuid NOT NULL REFERENCES "emotion_tag_groups" ("id") ON DELETE CASCADE,
    "display_order" integer NOT NULL DEFAULT 0,
    CONSTRAINT "ck_emotion_tag_group_members_display_order" CHECK (display_order >= 0)
);
COMMENT ON TABLE "emotion_tag_group_members" IS '감정 태그 하나가 속하는 표시 그룹 (태그당 그룹 하나)';
COMMENT ON COLUMN "emotion_tag_group_members"."tag_id" IS '감정 태그 ID (EMOTION 분류 tags.id)';
COMMENT ON COLUMN "emotion_tag_group_members"."group_id" IS '소속 그룹';
COMMENT ON COLUMN "emotion_tag_group_members"."display_order" IS '그룹 내 표시 순서';
CREATE INDEX "ix_emotion_tag_group_members_group" ON "emotion_tag_group_members" ("group_id", "display_order");
