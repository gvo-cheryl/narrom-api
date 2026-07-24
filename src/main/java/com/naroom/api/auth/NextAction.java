package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.Member;

// authentication.md 앱 시작 판정 순서 4단계 기준. 삭제 대기·잠금은 nextAction이 아니라 오류로 응답한다.
public enum NextAction {
	COMPLETE_ONBOARDING,
	ENTER_APP;

	public static NextAction forMember(Member member) {
		return member.getOnboardingCompletedAt() == null ? COMPLETE_ONBOARDING : ENTER_APP;
	}
}
