-- Naroom Beta 1 뱃지 초기 세트 (docs/instruction/badge/Naroom_Beta1_뱃지_기획_설계.md §7)

INSERT INTO badge_definitions
    (id, code, category, title, description, display_order)
VALUES
    ('1a1e6f1a-1c1a-4a11-8a11-000000000001'::uuid, 'FIRST_ENTRY', 'TRIAL', '첫 기록', '나로움에 처음으로 마음을 기록했어요.', 1),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000002'::uuid, 'FIRST_CHECKIN', 'TRIAL', '첫 체크인', '오늘의 감정과 에너지를 처음 확인했어요.', 2),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000003'::uuid, 'FIRST_EXPERIMENT_START', 'TRIAL', '첫 작은 실험', '작은 실험을 처음 시작했어요.', 3),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000004'::uuid, 'FIRST_WEEKLY_REFLECTION', 'DISCOVERY', '첫 주간 회고', '한 주의 기록을 처음 돌아봤어요.', 4),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000005'::uuid, 'FIRST_EXPERIMENT_REVIEW', 'DISCOVERY', '첫 코스 돌아보기', '작은 실험 코스를 마치고 처음 돌아봤어요.', 5),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000006'::uuid, 'FIRST_PERSONAL_SUMMARY', 'DISCOVERY', '첫 나의 정리', '지금의 나를 자신의 언어로 처음 정리했어요.', 6),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000007'::uuid, 'RETURN_AFTER_GAP', 'RETURN', '다시 만나서 반가워요', '며칠 만에 다시 돌아와 기록을 남겼어요.', 7),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000008'::uuid, 'RESUME_PAUSED_EXPERIMENT', 'RETURN', '실험을 다시 이어가요', '쉬어가던 작은 실험을 다시 이어가기 시작했어요.', 8),
    ('1a1e6f1a-1c1a-4a11-8a11-000000000009'::uuid, 'FIRST_SELF_REFLECTION', 'SELF_ORGANIZATION', '첫 내 생각 추가', 'AI의 정리에 나의 생각을 처음 더했어요.', 9),
    ('1a1e6f1a-1c1a-4a11-8a11-00000000000a'::uuid, 'SELF_REFLECTION_5', 'SELF_ORGANIZATION', '내 생각 다섯 번', '다섯 번의 기록에 나의 생각을 더했어요.', 10);
