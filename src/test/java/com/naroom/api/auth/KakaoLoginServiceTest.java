package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.DeviceInfo;
import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.kakao.KakaoClient;
import com.naroom.api.auth.kakao.KakaoUserInfoResponse;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DirtiesContext
class KakaoLoginServiceTest {

	@Autowired
	private KakaoLoginService kakaoLoginService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SocialIdentityRepository socialIdentityRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private KakaoClient kakaoClient;

	@Test
	void newKakaoUser_createsMemberAndReturnsCompleteOnboarding() {
		String providerUserId = String.valueOf(System.nanoTime());
		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		SocialLoginResponse response = kakaoLoginService.login(loginRequest("installation-new-" + providerUserId));

		assertEquals(NextAction.COMPLETE_ONBOARDING, response.nextAction());
		assertEquals(MemberStatus.ACTIVE, response.account().status());
		assertNotNull(socialIdentityRepository
				.findByProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId)
				.orElseThrow());
	}

	@Test
	void existingKakaoUser_reusesMemberAndReturnsEnterApp() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));
		markOnboardingCompleted(member);

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		SocialLoginResponse response = kakaoLoginService.login(loginRequest("installation-existing-" + providerUserId));

		assertEquals(NextAction.ENTER_APP, response.nextAction());
		assertEquals(member.getId(), response.account().memberId());
	}

	// 같은 기기(installationKey)로 다른 회원이 로그인하는 경우 - 기기 공유/계정 전환 시나리오.
	// DeviceInstallationService.registerOrReuseDevice가 이 기기를 새 회원 소유로 옮기고, 옛 회원이 이
	// 기기로 발급받았던 세션을 폐기하지 않으면 이후 푸시 토큰 갱신이 DEVICE_INSTALLATION_NOT_FOUND로
	// 실패한다. 옛 회원이 등록해 둔 푸시 토큰도 새 회원에게 그대로 넘어가면 안 된다.
	@Test
	void sameInstallationKey_differentMember_reassignsDeviceAndRevokesOldMemberSessions() {
		String firstProviderUserId = String.valueOf(System.nanoTime());
		String secondProviderUserId = String.valueOf(System.nanoTime() + 1);
		String installationKey = "installation-shared-" + firstProviderUserId;

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(firstProviderUserId, "먼저"));
		SocialLoginResponse firstLogin = kakaoLoginService.login(loginRequest(installationKey));
		deviceInstallationRepository.findByInstallationKey(installationKey)
				.orElseThrow()
				.updatePushToken("first-member-push-token-ciphertext");

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(secondProviderUserId, "나중"));
		SocialLoginResponse secondLogin = kakaoLoginService.login(loginRequest(installationKey));

		DeviceInstallation device = deviceInstallationRepository.findByInstallationKey(installationKey).orElseThrow();
		assertEquals(secondLogin.account().memberId(), device.getMember().getId());
		assertNull(device.getPushTokenCiphertext());

		List<AuthSession> firstMemberActiveSessions =
				authSessionRepository.findByMember_IdAndRevokedAtIsNull(firstLogin.account().memberId());
		assertTrue(firstMemberActiveSessions.isEmpty());
	}

	@Test
	void lockedMember_throwsAccountLocked() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));
		updateMemberStatus(member, MemberStatus.LOCKED);

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(loginRequest("installation-locked-" + providerUserId)));
		assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.errorCode());
	}

	@Test
	void pendingDeletionMember_throwsAccountPendingDeletion() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));
		entityManager.createQuery(
						"update Member m set m.status = :status, m.scheduledDeletionAt = :scheduledDeletionAt where m.id = :id")
				.setParameter("status", MemberStatus.PENDING_DELETION)
				.setParameter("scheduledDeletionAt", Instant.now().plusSeconds(604_800))
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(loginRequest("installation-pending-deletion-" + providerUserId)));
		assertEquals(AuthErrorCode.ACCOUNT_PENDING_DELETION, exception.errorCode());
	}

	@Test
	void revokedSocialIdentity_throwsSocialIdentityRevoked() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		SocialIdentity socialIdentity = socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));
		entityManager.createNativeQuery("update social_identities set status = 'REVOKED'::identity_status where id = :id")
				.setParameter("id", socialIdentity.getId())
				.executeUpdate();
		entityManager.clear();

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(loginRequest("installation-revoked-" + providerUserId)));
		assertEquals(AuthErrorCode.AUTH_SOCIAL_IDENTITY_REVOKED, exception.errorCode());
	}

	@Test
	void missingInstallationKey_throwsDeviceInstallationKeyRequired() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(new KakaoLoginRequest(
						"kakao-token", new DeviceInfo("", "IOS", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED, exception.errorCode());
	}

	@Test
	void unsupportedPlatform_throwsDevicePlatformUnsupported() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(new KakaoLoginRequest(
						"kakao-token", new DeviceInfo("installation-bad-platform", "WEB", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_PLATFORM_UNSUPPORTED, exception.errorCode());
	}

	@Test
	void restore_pendingDeletionMember_restoresAndReturnsSession() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));
		entityManager.createQuery(
						"update Member m set m.status = :status, m.scheduledDeletionAt = :scheduledDeletionAt where m.id = :id")
				.setParameter("status", MemberStatus.PENDING_DELETION)
				.setParameter("scheduledDeletionAt", Instant.now().plusSeconds(604_800))
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		SocialLoginResponse response = kakaoLoginService.restore(loginRequest("installation-restore-" + providerUserId));

		assertEquals(MemberStatus.ACTIVE, response.account().status());
		assertNotNull(response.accessToken());
		Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
		assertEquals(MemberStatus.ACTIVE, reloaded.getStatus());
	}

	@Test
	void restore_activeMember_throwsAccountNotPendingDeletion() {
		String providerUserId = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.KAKAO, providerUserId, null, false, "지연", null));

		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.restore(loginRequest("installation-restore-active-" + providerUserId)));
		assertEquals(AuthErrorCode.ACCOUNT_NOT_PENDING_DELETION, exception.errorCode());
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

	private KakaoLoginRequest loginRequest(String installationKey) {
		return new KakaoLoginRequest(
				"kakao-provider-token", new DeviceInfo(installationKey, "IOS", "1.0.0"));
	}

	private KakaoUserInfoResponse kakaoUser(String providerUserId, String nickname) {
		return new KakaoUserInfoResponse(
				Long.valueOf(providerUserId),
				new KakaoUserInfoResponse.KakaoAccount(
						null, false, new KakaoUserInfoResponse.KakaoAccount.Profile(nickname, null)));
	}

}
