-- Naroom Beta 1 체크인 감정 태그 초기 데이터
-- PostgreSQL / Flyway repeatable 또는 버전 마이그레이션용
--
-- 총 6개 화면 카테고리, 48개 시스템 감정 태그
-- 화면 카테고리는 현재 tags 테이블의 별도 컬럼이 아니므로 주석과 입력 순서로만 구분한다.
-- 실제 선택 결과는 check_in_emotions(check_in_id, tag_id)에 저장한다.

BEGIN;

INSERT INTO tags (
    owner_member_id,
    scope,
    category,
    name,
    normalized_name,
    active
)
VALUES
    -- 1. 편안한 결 (9)
    (NULL, 'SYSTEM', 'EMOTION', '편안함', '편안함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '안도감', '안도감', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '고마움', '고마움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '차분함', '차분함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '홀가분함', '홀가분함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '다정함', '다정함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '만족스러움', '만족스러움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '포근함', '포근함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '느긋함', '느긋함', TRUE),

    -- 2. 기운이 도는 (9)
    (NULL, 'SYSTEM', 'EMOTION', '기쁨', '기쁨', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '설렘', '설렘', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '기대', '기대', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '즐거움', '즐거움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '자신감', '자신감', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '뿌듯함', '뿌듯함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '후련함', '후련함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '벅참', '벅참', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '활기', '활기', TRUE),

    -- 3. 가라앉는 (9)
    (NULL, 'SYSTEM', 'EMOTION', '지침', '지침', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '무기력', '무기력', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '외로움', '외로움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '허전함', '허전함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '슬픔', '슬픔', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '서운함', '서운함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '아쉬움', '아쉬움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '그리움', '그리움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '서글픔', '서글픔', TRUE),

    -- 4. 조여드는 (8)
    (NULL, 'SYSTEM', 'EMOTION', '불안', '불안', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '초조함', '초조함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '긴장', '긴장', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '부담', '부담', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '조심스러움', '조심스러움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '두려움', '두려움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '걱정', '걱정', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '막막함', '막막함', TRUE),

    -- 5. 뜨거워지는 (7)
    (NULL, 'SYSTEM', 'EMOTION', '답답함', '답답함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '짜증', '짜증', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '화남', '화남', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '억울함', '억울함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '서러움', '서러움', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '민망함', '민망함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '분함', '분함', TRUE),

    -- 6. 이름 붙이기 어려운 (6)
    (NULL, 'SYSTEM', 'EMOTION', '무덤덤함', '무덤덤함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '복합적인 느낌', '복합적인 느낌', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '멍함', '멍함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '낯섦', '낯섦', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '궁금함', '궁금함', TRUE),
    (NULL, 'SYSTEM', 'EMOTION', '싱숭생숭함', '싱숭생숭함', TRUE)
ON CONFLICT (category, normalized_name) WHERE scope = 'SYSTEM'
DO UPDATE
SET name = EXCLUDED.name,
    active = EXCLUDED.active,
    updated_at = now();

COMMIT;

-- 적용 확인: 결과가 48이어야 한다.
SELECT COUNT(*) AS system_emotion_tag_count
FROM tags
WHERE scope = 'SYSTEM'
  AND category = 'EMOTION'
  AND normalized_name IN (
      '편안함', '안도감', '고마움', '차분함', '홀가분함', '다정함', '만족스러움', '포근함', '느긋함',
      '기쁨', '설렘', '기대', '즐거움', '자신감', '뿌듯함', '후련함', '벅참', '활기',
      '지침', '무기력', '외로움', '허전함', '슬픔', '서운함', '아쉬움', '그리움', '서글픔',
      '불안', '초조함', '긴장', '부담', '조심스러움', '두려움', '걱정', '막막함',
      '답답함', '짜증', '화남', '억울함', '서러움', '민망함', '분함',
      '무덤덤함', '복합적인 느낌', '멍함', '낯섦', '궁금함', '싱숭생숭함'
  );
