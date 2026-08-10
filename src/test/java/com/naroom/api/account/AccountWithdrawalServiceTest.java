package com.naroom.api.account;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.AccountWithdrawalResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class AccountWithdrawalServiceTest {

	@Autowired
	private AccountWithdrawalService accountWithdrawalService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Test
	void requestWithdrawal_activeMember_transitionsToPendingDeletion() {
		Member member = memberRepository.save(Member.create("지연"));

		AccountWithdrawalResponse response = accountWithdrawalService.requestWithdrawal(member.getId());

		Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
		assertEquals(MemberStatus.PENDING_DELETION, reloaded.getStatus());
		assertEquals(response.scheduledDeletionAt(), reloaded.getScheduledDeletionAt());
	}

	@Test
	void requestWithdrawal_revokesAllActiveSessions() {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device =
				deviceInstallationRepository.save(DeviceInstallation.register(member, "device-1", "IOS", "1.0.0"));
		AuthSession session = authSessionRepository.save(
				AuthSession.issue(member, device, "hash-1", Instant.now().plusSeconds(3600)));

		accountWithdrawalService.requestWithdrawal(member.getId());

		AuthSession reloaded = authSessionRepository.findById(session.getId()).orElseThrow();
		assertNotNull(reloaded.getRevokedAt());
	}

	@Test
	void requestWithdrawal_alreadyPendingDeletion_throwsAccountNotActive() {
		Member member = memberRepository.save(Member.create("지연"));
		accountWithdrawalService.requestWithdrawal(member.getId());

		BusinessException exception = assertThrows(
				BusinessException.class, () -> accountWithdrawalService.requestWithdrawal(member.getId()));
		assertEquals(AccountErrorCode.ACCOUNT_NOT_ACTIVE, exception.errorCode());
	}

}
