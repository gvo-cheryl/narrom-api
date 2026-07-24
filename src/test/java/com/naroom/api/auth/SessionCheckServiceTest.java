package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.SessionCheckResponse;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class SessionCheckServiceTest {

	@Autowired
	private SessionCheckService sessionCheckService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private AuthSessionService authSessionService;

	@Autowired
	private EntityManager entityManager;

	@Test
	void onboardingNotCompleted_returnsCompleteOnboarding() {
		Member member = memberRepository.save(Member.create("지연"));
		AuthSession session = issueSession(member);

		SessionCheckResponse response = sessionCheckService.check(member.getId(), session.getId());

		assertTrue(response.authenticated());
		assertEquals(NextAction.COMPLETE_ONBOARDING, response.nextAction());
		assertEquals(member.getId(), response.account().memberId());
		assertEquals(session.getId(), response.session().id());
	}

	@Test
	void onboardingCompleted_returnsEnterApp() {
		Member member = memberRepository.save(Member.create("지연"));
		markOnboardingCompleted(member);
		AuthSession session = issueSession(member);

		SessionCheckResponse response = sessionCheckService.check(member.getId(), session.getId());

		assertEquals(NextAction.ENTER_APP, response.nextAction());
	}

	@Test
	void lockedMember_throwsAccountLocked() {
		Member member = memberRepository.save(Member.create("지연"));
		AuthSession session = issueSession(member);
		updateMemberStatus(member, MemberStatus.LOCKED);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> sessionCheckService.check(member.getId(), session.getId()));
		assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.errorCode());
	}

	@Test
	void pendingDeletionMember_throwsAccountPendingDeletion() {
		Member member = memberRepository.save(Member.create("지연"));
		AuthSession session = issueSession(member);
		entityManager.createQuery(
						"update Member m set m.status = :status, m.scheduledDeletionAt = :scheduledDeletionAt where m.id = :id")
				.setParameter("status", MemberStatus.PENDING_DELETION)
				.setParameter("scheduledDeletionAt", Instant.now().plusSeconds(604_800))
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> sessionCheckService.check(member.getId(), session.getId()));
		assertEquals(AuthErrorCode.ACCOUNT_PENDING_DELETION, exception.errorCode());
	}

	private AuthSession issueSession(Member member) {
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, "installation-" + UUID.randomUUID(), "IOS", "1.0.0"));
		return authSessionRepository.findById(authSessionService.issue(member, device).session().getId())
				.orElseThrow();
	}

	private void markOnboardingCompleted(Member member) {
		entityManager.createQuery("update Member m set m.onboardingCompletedAt = :now where m.id = :id")
				.setParameter("now", Instant.now())
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();
	}

	private void updateMemberStatus(Member member, MemberStatus status) {
		entityManager.createQuery("update Member m set m.status = :status where m.id = :id")
				.setParameter("status", status)
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();
	}

}
