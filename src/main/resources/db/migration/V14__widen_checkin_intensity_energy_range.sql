-- Naroom Beta 1
-- 체크인 화면에서 감정 강도·에너지를 5단계 버튼이 아니라 드래그 슬라이더로 바꾸면서(FE),
-- 더 세밀한 값을 받기 위해 저장 범위를 1~5에서 0~100으로 넓힌다. 화면에는 여전히
-- 거의 없음/조금/보통/크게/아주 크게 5단계로만 보여주고(20%씩 균등 구간), 저장값만 더 촘촘해진다.
-- 컬럼 타입(smallint)은 0~100을 담기에 충분해 그대로 두고 CHECK 제약만 넓힌다.

ALTER TABLE "check_ins" DROP CONSTRAINT "ck_check_ins_1";
ALTER TABLE "check_ins" DROP CONSTRAINT "ck_check_ins_2";

ALTER TABLE "check_ins" ADD CONSTRAINT "ck_check_ins_1" CHECK (emotion_intensity IS NULL OR emotion_intensity BETWEEN 0 AND 100);
ALTER TABLE "check_ins" ADD CONSTRAINT "ck_check_ins_2" CHECK (energy_level IS NULL OR energy_level BETWEEN 0 AND 100);

COMMENT ON COLUMN "check_ins"."emotion_intensity" IS '감정 강도 0~100(화면에는 5단계로만 표시)';
COMMENT ON COLUMN "check_ins"."energy_level" IS '에너지 수준 0~100(화면에는 5단계로만 표시)';
