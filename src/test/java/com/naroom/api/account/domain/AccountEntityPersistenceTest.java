package com.naroom.api.account.domain;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.IdentityStatus;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberConsent;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberConsentRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Postgres native enum(social_provider 등) 매핑처럼 스키마 검증만으로는 확인되지 않는
 * 실제 저장/조회 왕복을 검증한다. 각 테스트는 트랜잭션 롤백으로 데이터를 남기지 않는다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class AccountEntityPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SocialIdentityRepository socialIdentityRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private MemberConsentRepository memberConsentRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void accountAggregate_roundTripsThroughAllTables() {
		Member member = memberRepository.save(Member.create("지연"));

		String providerUserId = UUID.randomUUID().toString();
		SocialIdentity socialIdentity =
				socialIdentityRepository.save(SocialIdentity.connect(
						member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));

		String installationKey = UUID.randomUUID().toString();
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));

		AuthSession session = authSessionRepository.save(
				AuthSession.issue(member, device, UUID.randomUUID().toString(), Instant.now().plus(30, ChronoUnit.DAYS)));

		MemberConsent consent = memberConsentRepository.save(
				MemberConsent.record(member, ConsentType.TERMS, "1.0", true, "ONBOARDING"));

		NotificationPreference preference =
				notificationPreferenceRepository.save(
						NotificationPreference.createDisabled(member, NotificationType.WEEKLY_REFLECTION));
		preference.update(true, LocalTime.of(9, 0), (short) 1);

		entityManager.flush();
		entityManager.clear();

		Member savedMember = memberRepository.findById(member.getId()).orElseThrow();
		assertEquals("지연", savedMember.getDisplayName());
		assertEquals(MemberStatus.ACTIVE, savedMember.getStatus());
		assertEquals("Asia/Seoul", savedMember.getTimezone());
		assertNotNull(savedMember.getCreatedAt());
		assertNotNull(savedMember.getVersion());

		SocialIdentity savedIdentity =
				socialIdentityRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId)
						.orElseThrow();
		assertEquals(socialIdentity.getId(), savedIdentity.getId());
		assertEquals(IdentityStatus.ACTIVE, savedIdentity.getStatus());

		DeviceInstallation savedDevice =
				deviceInstallationRepository.findByInstallationKey(installationKey).orElseThrow();
		assertEquals("IOS", savedDevice.getPlatform());

		AuthSession savedSession = authSessionRepository.findById(session.getId()).orElseThrow();
		assertEquals(device.getId(), savedSession.getDeviceInstallation().getId());
		assertTrue(savedSession.getExpiresAt().isAfter(Instant.now()));

		MemberConsent savedConsent = memberConsentRepository.findById(consent.getId()).orElseThrow();
		assertEquals(ConsentType.TERMS, savedConsent.getConsentType());
		assertTrue(savedConsent.isAgreed());

		NotificationPreference savedPreference = notificationPreferenceRepository
				.findByMemberIdAndNotificationType(member.getId(), NotificationType.WEEKLY_REFLECTION)
				.orElseThrow();
		assertEquals(preference.getId(), savedPreference.getId());
		assertTrue(savedPreference.isEnabled());
		assertEquals(LocalTime.of(9, 0), savedPreference.getLocalTime());
		assertEquals((short) 1, savedPreference.getDayOfWeek());
	}

}
