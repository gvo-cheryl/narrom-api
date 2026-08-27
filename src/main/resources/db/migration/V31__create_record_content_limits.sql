CREATE TABLE "record_content_limits" (
	"id" uuid PRIMARY KEY,
	"body_max_length" integer NOT NULL,
	"updated_by_admin_id" uuid,
	"updated_at" timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE "record_content_limits" IS '기록 본문(entries.body) 최대 글자 수 - 관리자 웹에서 수정하는 단일 행 설정. id는 애플리케이션이 고정하는 값이라 여러 행이 생기지 않는다.';

INSERT INTO "record_content_limits" ("id", "body_max_length")
VALUES ('00000000-0000-0000-0000-000000000001', 2000);
