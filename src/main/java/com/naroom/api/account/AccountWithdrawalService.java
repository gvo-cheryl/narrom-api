package com.naroom.api.account;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.AccountWithdrawalResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// Account Deletion Rules: 요청 즉시 접근을 막고(PENDING_DELETION 전환 + 세션 전체 revoke), 7일 그레이스
// 기간 뒤에 영구 삭제한다. 그레이스 기간 안의 명시적 복구는 KakaoLoginService.restore()가 담당한다
// (로그인만으로 자동 복구되지 않는다).
@Service
@Transactional(readOnly = true)
public class AccountWithdrawalService {

	private static final Duration GRACE_PERIOD = Duration.ofDays(7);
	private static final String WITHDRAWAL_REVOKE_REASON = "WITHDRAWAL";

	private final MemberRepository memberRepository;
	private final AuthSessionRepository authSessionRepository;

	public AccountWithdrawalService(MemberRepository memberRepository, AuthSessionRepository authSessionRepository) {
		this.memberRepository = memberRepository;
		this.authSessionRepository = authSessionRepository;
	}

	@Transactional
	public AccountWithdrawalResponse requestWithdrawal(UUID memberId) {
		Member member = memberRepository.getReferenceById(memberId);
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_ACTIVE);
		}

		Instant now = Instant.now();
		Instant scheduledDeletionAt = now.plus(GRACE_PERIOD);
		member.requestWithdrawal(now, scheduledDeletionAt);

		authSessionRepository.revokeAll(
				authSessionRepository.findByMember_IdAndRevokedAtIsNull(memberId), WITHDRAWAL_REVOKE_REASON);

		return new AccountWithdrawalResponse(scheduledDeletionAt);
	}

}
