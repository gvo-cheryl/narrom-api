package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.SessionCheckResponse;
import com.naroom.api.auth.dto.SessionSummary;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// authentication.md 서버 세션 확인 처리 순서를 따른다. JwtAuthenticationFilter가 서명·세션 유효성을
// 이미 검증했으므로 여기서는 회원 상태 재확인과 온보딩 분기만 담당한다.
@Service
public class SessionCheckService {

	private final MemberRepository memberRepository;
	private final AuthSessionRepository authSessionRepository;
	private final AuthSessionService authSessionService;

	public SessionCheckService(
			MemberRepository memberRepository,
			AuthSessionRepository authSessionRepository,
			AuthSessionService authSessionService) {
		this.memberRepository = memberRepository;
		this.authSessionRepository = authSessionRepository;
		this.authSessionService = authSessionService;
	}

	@Transactional(readOnly = true)
	public SessionCheckResponse check(UUID memberId, UUID sessionId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalStateException("Authenticated member not found: " + memberId));
		authSessionService.requireLoginableStatus(member);

		AuthSession session = authSessionRepository.findById(sessionId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_SESSION_NOT_FOUND));

		return new SessionCheckResponse(
				true,
				new SessionSummary(session.getId(), session.getExpiresAt()),
				new AccountSummary(
						member.getId(),
						member.getDisplayName(),
						member.getStatus(),
						member.getOnboardingCompletedAt(),
						member.getVersion()),
				NextAction.forMember(member));
	}

}
