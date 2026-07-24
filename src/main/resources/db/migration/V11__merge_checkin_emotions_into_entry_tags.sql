-- Naroom Beta 1
-- 체크인 감정을 별도 check_in_emotions 테이블 대신 entry_tags로 통합한다(entry_tags.source = 'CHECK_IN').
-- AI가 감정을 추천하기 시작하면 체크인 감정과 AI 추천 감정이 서로 다른 테이블에 나뉘는 문제를 막기 위함
-- (0725_AI-Domain-Schema-Review-Report.md 5.1 결정). tag_source.CHECK_IN은 V2부터 이미 존재해
-- 이 통합을 염두에 둔 설계였다. 아직 실제 데이터가 없어 백필 없이 바로 적용한다.
--
-- 동시에 entry_tags.source(생성 기능 출처)와는 다른 축인 "제안 주체"를 initiated_by 컬럼으로 추가한다
-- (같은 보고서 5.3 결정: source enum을 바꾸지 않고 컬럼을 추가하는 절충안).

CREATE TYPE "tag_initiator" AS ENUM ('USER_SELECTED', 'USER_ENTERED', 'AI_INFERRED');

ALTER TABLE "entry_tags" ADD COLUMN "initiated_by" tag_initiator NOT NULL DEFAULT 'USER_SELECTED';
ALTER TABLE "entry_tags" ALTER COLUMN "initiated_by" DROP DEFAULT;
COMMENT ON COLUMN "entry_tags"."initiated_by" IS '태그를 처음 제안한 주체(선택/직접 입력/AI 추정); source(생성 기능 출처)와는 다른 축';

DROP TABLE "check_in_emotions";
