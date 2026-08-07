ALTER TABLE "notification_preferences" ADD COLUMN "last_sent_at" timestamptz;
COMMENT ON COLUMN "notification_preferences"."last_sent_at" IS '발송 스케줄러가 마지막으로 보낸 시각(회원 시간대 기준 하루 1회 중복 발송 방지용)';
