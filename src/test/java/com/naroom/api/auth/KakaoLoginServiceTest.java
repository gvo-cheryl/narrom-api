package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.KakaoLoginResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
	private EntityManager entityManager;

	@MockitoBean
	private KakaoClient kakaoClient;

	@Test
	void newKakaoUser_createsMemberAndReturnsCompleteOnboarding() {
		String providerUserId = String.valueOf(System.nanoTime());
		when(kakaoClient.fetchUserInfo(any())).thenReturn(kakaoUser(providerUserId, "지연"));

		KakaoLoginResponse response = kakaoLoginService.login(loginRequest("installation-new-" + providerUserId));

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

		KakaoLoginResponse response = kakaoLoginService.login(loginRequest("installation-existing-" + providerUserId));

		assertEquals(NextAction.ENTER_APP, response.nextAction());
		assertEquals(member.getId(), response.account().memberId());
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
						"kakao-token", new KakaoLoginRequest.DeviceInfo("", "IOS", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED, exception.errorCode());
	}

	@Test
	void unsupportedPlatform_throwsDevicePlatformUnsupported() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> kakaoLoginService.login(new KakaoLoginRequest(
						"kakao-token", new KakaoLoginRequest.DeviceInfo("installation-bad-platform", "WEB", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_PLATFORM_UNSUPPORTED, exception.errorCode());
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
				"kakao-provider-token", new KakaoLoginRequest.DeviceInfo(installationKey, "IOS", "1.0.0"));
	}

	private KakaoUserInfoResponse kakaoUser(String providerUserId, String nickname) {
		return new KakaoUserInfoResponse(
				Long.valueOf(providerUserId),
				new KakaoUserInfoResponse.KakaoAccount(
						null, false, new KakaoUserInfoResponse.KakaoAccount.Profile(nickname, null)));
	}

}
