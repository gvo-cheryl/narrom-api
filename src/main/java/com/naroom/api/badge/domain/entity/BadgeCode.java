package com.naroom.api.badge.domain.entity;

// docs/instruction/badge/Naroom_Beta1_뱃지_기획_설계.md §7 초기 세트. 코드 추가 시 badge_code(Postgres
// ENUM)와 badge_definitions 시드 데이터(V19)도 함께 늘려야 한다.
public enum BadgeCode {
	FIRST_ENTRY,
	FIRST_CHECKIN,
	FIRST_EXPERIMENT_START,
	FIRST_WEEKLY_REFLECTION,
	FIRST_EXPERIMENT_REVIEW,
	FIRST_PERSONAL_SUMMARY,
	RETURN_AFTER_GAP,
	RESUME_PAUSED_EXPERIMENT,
	FIRST_SELF_REFLECTION,
	SELF_REFLECTION_5
}
