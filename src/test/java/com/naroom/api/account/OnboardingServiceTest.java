package com.naroom.api.account;

import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.MemberConsentRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.account.dto.OnboardingCompleteRequest;
import com.naroom.api.account.dto.OnboardingCompleteResponse;
import com.naroom.api.auth.NextAction;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class OnboardingServiceTest {

	@Autowired
	private OnboardingService onboardingService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberConsentRepository memberConsentRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void validRequest_completesOnboardingAndReturnsEnterApp() {
		Member member = memberRepository.save(Member.create("나로움"));

		OnboardingCompleteResponse response = onboardingService.complete(member.getId(), validRequest(0L));

		assertEquals(NextAction.ENTER_APP, response.nextAction());
		assertNotNull(response.account().onboardingCompletedAt());
		assertEquals(1L, response.account().version());

		Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
		assertEquals("지연", reloaded.getDisplayName());
		assertEquals(3, memberConsentRepository.findAll().stream()
				.filter(consent -> consent.getMember().getId().equals(member.getId()))
				.count());
		assertEquals(1, notificationPreferenceRepository
				.findByMemberIdAndNotificationType(member.getId(), NotificationType.WEEKLY_REFLECTION)
				.stream().count());
	}

	@Test
	void versionMismatch_throwsAccountVersionConflict() {
		Member member = memberRepository.save(Member.create("나로움"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> onboardingService.complete(member.getId(), validRequest(99L)));
		assertEquals(AccountErrorCode.ACCOUNT_VERSION_CONFLICT, exception.errorCode());
	}

	@Test
	void replayOfCompletedRequest_returnsCurrentStateWithoutDuplicatingConsents() {
		Member member = memberRepository.save(Member.create("나로움"));
		onboardingService.complete(member.getId(), validRequest(0L));

		OnboardingCompleteResponse replay = onboardingService.complete(member.getId(), validRequest(0L));

		assertEquals(NextAction.ENTER_APP, replay.nextAction());
		assertEquals(3, memberConsentRepository.findAll().stream()
				.filter(consent -> consent.getMember().getId().equals(member.getId()))
				.count());
	}

	@Test
	void missingRequiredConsent_throwsOnboardingConsentRequired() {
		Member member = memberRepository.save(Member.create("나로움"));
		OnboardingCompleteRequest request = new OnboardingCompleteRequest(
				0L,
				"지연",
				"Asia/Seoul",
				"ko-KR",
				List.of(
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.TERMS, "1.0", true),
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.PRIVACY, "1.0", true)),
				List.of());

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> onboardingService.complete(member.getId(), request));
		assertEquals(AccountErrorCode.ONBOARDING_CONSENT_REQUIRED, exception.errorCode());
	}

	@Test
	void unsupportedDocumentVersion_throwsOnboardingDocumentVersionInvalid() {
		Member member = memberRepository.save(Member.create("나로움"));
		OnboardingCompleteRequest request = new OnboardingCompleteRequest(
				0L,
				"지연",
				"Asia/Seoul",
				"ko-KR",
				List.of(
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.TERMS, "0.9", true),
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.PRIVACY, "1.0", true),
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.AI_PROCESSING, "1.0", true)),
				List.of());

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> onboardingService.complete(member.getId(), request));
		assertEquals(AccountErrorCode.ONBOARDING_DOCUMENT_VERSION_INVALID, exception.errorCode());
	}

	@Test
	void lockedMember_throwsAccountLocked() {
		Member member = memberRepository.save(Member.create("나로움"));
		entityManager.createQuery("update Member m set m.status = :status where m.id = :id")
				.setParameter("status", MemberStatus.LOCKED)
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> onboardingService.complete(member.getId(), validRequest(0L)));
		assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.errorCode());
	}

	private OnboardingCompleteRequest validRequest(Long version) {
		return new OnboardingCompleteRequest(
				version,
				"지연",
				"Asia/Seoul",
				"ko-KR",
				List.of(
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.TERMS, "1.0", true),
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.PRIVACY, "1.0", true),
						new OnboardingCompleteRequest.ConsentRequest(ConsentType.AI_PROCESSING, "1.0", true)),
				List.of(new OnboardingCompleteRequest.NotificationPreferenceRequest(
						NotificationType.WEEKLY_REFLECTION, true, 1, LocalTime.of(9, 0))));
	}

}
