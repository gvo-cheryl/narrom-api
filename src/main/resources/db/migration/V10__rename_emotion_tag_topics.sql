-- 명명 규칙을 Content 도메인의 quotes/quote_topics/quote_topic_links 패턴에 맞춘다.
-- V8/V9는 이미 적용된 마이그레이션이라 직접 수정하지 않고 리네이밍만 새 마이그레이션으로 추가한다.
-- (데이터는 그대로 유지되고 테이블·컬럼·제약조건 이름만 group → topic으로 바뀐다.)

ALTER TABLE "emotion_tag_groups" RENAME TO "emotion_tag_topics";
ALTER TABLE "emotion_tag_group_members" RENAME TO "emotion_tag_topic_links";
ALTER TABLE "emotion_tag_topic_links" RENAME COLUMN "group_id" TO "topic_id";

ALTER TABLE "emotion_tag_topics" RENAME CONSTRAINT "emotion_tag_groups_pkey" TO "emotion_tag_topics_pkey";
ALTER TABLE "emotion_tag_topics" RENAME CONSTRAINT "emotion_tag_groups_code_key" TO "emotion_tag_topics_code_key";
ALTER TABLE "emotion_tag_topics" RENAME CONSTRAINT "ck_emotion_tag_groups_display_order" TO "ck_emotion_tag_topics_display_order";

ALTER TABLE "emotion_tag_topic_links" RENAME CONSTRAINT "emotion_tag_group_members_pkey" TO "emotion_tag_topic_links_pkey";
ALTER TABLE "emotion_tag_topic_links" RENAME CONSTRAINT "emotion_tag_group_members_tag_id_fkey" TO "emotion_tag_topic_links_tag_id_fkey";
ALTER TABLE "emotion_tag_topic_links" RENAME CONSTRAINT "emotion_tag_group_members_group_id_fkey" TO "emotion_tag_topic_links_topic_id_fkey";
ALTER TABLE "emotion_tag_topic_links" RENAME CONSTRAINT "ck_emotion_tag_group_members_display_order" TO "ck_emotion_tag_topic_links_display_order";

ALTER INDEX "ix_emotion_tag_group_members_group" RENAME TO "ix_emotion_tag_topic_links_topic";

COMMENT ON TABLE "emotion_tag_topics" IS '체크인 감정 선택 화면에 쓰이는 감정 태그 표시 주제';
COMMENT ON COLUMN "emotion_tag_topics"."id" IS '감정 태그 주제 ID';
COMMENT ON COLUMN "emotion_tag_topics"."code" IS '변하지 않는 주제 코드';
COMMENT ON COLUMN "emotion_tag_topics"."name" IS '사용자 표시 주제명';
COMMENT ON COLUMN "emotion_tag_topics"."display_order" IS '화면 표시 순서';

COMMENT ON TABLE "emotion_tag_topic_links" IS '감정 태그 하나가 속하는 표시 주제 (태그당 주제 하나)';
COMMENT ON COLUMN "emotion_tag_topic_links"."tag_id" IS '감정 태그 ID (EMOTION 분류 tags.id)';
COMMENT ON COLUMN "emotion_tag_topic_links"."topic_id" IS '소속 주제';
COMMENT ON COLUMN "emotion_tag_topic_links"."display_order" IS '주제 내 표시 순서';
