-- V6에서 넣은 48개 시스템 감정 태그를 원본 주석에 있던 6개 표시 그룹으로 매핑한다.
-- 1. 편안한 결 (9): 편안함, 안도감, 고마움, 차분함, 홀가분함, 다정함, 만족스러움, 포근함, 느긋함
-- 2. 기운이 도는 (9): 기쁨, 설렘, 기대, 즐거움, 자신감, 뿌듯함, 후련함, 벅참, 활기
-- 3. 가라앉는 (9): 지침, 무기력, 외로움, 허전함, 슬픔, 서운함, 아쉬움, 그리움, 서글픔
-- 4. 조여드는 (8): 불안, 초조함, 긴장, 부담, 조심스러움, 두려움, 걱정, 막막함
-- 5. 뜨거워지는 (7): 답답함, 짜증, 화남, 억울함, 서러움, 민망함, 분함
-- 6. 이름 붙이기 어려운 (6): 무덤덤함, 복합적인 느낌, 멍함, 낯섦, 궁금함, 싱숭생숭함

INSERT INTO "emotion_tag_groups" ("code", "name", "display_order") VALUES
    ('COMFORTABLE', '편안한 결', 1),
    ('ENERGIZED', '기운이 도는', 2),
    ('SINKING', '가라앉는', 3),
    ('TIGHTENING', '조여드는', 4),
    ('HEATED', '뜨거워지는', 5),
    ('HARD_TO_NAME', '이름 붙이기 어려운', 6)
ON CONFLICT ("code") DO NOTHING;

WITH member_data ("group_code", "tag_name", "display_order") AS (
    VALUES
        ('COMFORTABLE', '편안함', 1),
        ('COMFORTABLE', '안도감', 2),
        ('COMFORTABLE', '고마움', 3),
        ('COMFORTABLE', '차분함', 4),
        ('COMFORTABLE', '홀가분함', 5),
        ('COMFORTABLE', '다정함', 6),
        ('COMFORTABLE', '만족스러움', 7),
        ('COMFORTABLE', '포근함', 8),
        ('COMFORTABLE', '느긋함', 9),

        ('ENERGIZED', '기쁨', 1),
        ('ENERGIZED', '설렘', 2),
        ('ENERGIZED', '기대', 3),
        ('ENERGIZED', '즐거움', 4),
        ('ENERGIZED', '자신감', 5),
        ('ENERGIZED', '뿌듯함', 6),
        ('ENERGIZED', '후련함', 7),
        ('ENERGIZED', '벅참', 8),
        ('ENERGIZED', '활기', 9),

        ('SINKING', '지침', 1),
        ('SINKING', '무기력', 2),
        ('SINKING', '외로움', 3),
        ('SINKING', '허전함', 4),
        ('SINKING', '슬픔', 5),
        ('SINKING', '서운함', 6),
        ('SINKING', '아쉬움', 7),
        ('SINKING', '그리움', 8),
        ('SINKING', '서글픔', 9),

        ('TIGHTENING', '불안', 1),
        ('TIGHTENING', '초조함', 2),
        ('TIGHTENING', '긴장', 3),
        ('TIGHTENING', '부담', 4),
        ('TIGHTENING', '조심스러움', 5),
        ('TIGHTENING', '두려움', 6),
        ('TIGHTENING', '걱정', 7),
        ('TIGHTENING', '막막함', 8),

        ('HEATED', '답답함', 1),
        ('HEATED', '짜증', 2),
        ('HEATED', '화남', 3),
        ('HEATED', '억울함', 4),
        ('HEATED', '서러움', 5),
        ('HEATED', '민망함', 6),
        ('HEATED', '분함', 7),

        ('HARD_TO_NAME', '무덤덤함', 1),
        ('HARD_TO_NAME', '복합적인 느낌', 2),
        ('HARD_TO_NAME', '멍함', 3),
        ('HARD_TO_NAME', '낯섦', 4),
        ('HARD_TO_NAME', '궁금함', 5),
        ('HARD_TO_NAME', '싱숭생숭함', 6)
)
INSERT INTO "emotion_tag_group_members" ("tag_id", "group_id", "display_order")
SELECT t."id", g."id", md."display_order"
FROM member_data md
JOIN "emotion_tag_groups" g ON g."code" = md."group_code"
JOIN "tags" t ON t."scope" = 'SYSTEM' AND t."category" = 'EMOTION' AND t."normalized_name" = md."tag_name"
ON CONFLICT ("tag_id") DO UPDATE SET "group_id" = EXCLUDED."group_id", "display_order" = EXCLUDED."display_order";
