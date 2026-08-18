package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.DeviceInfo;
import com.naroom.api.auth.dto.GoogleLoginRequest;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.auth.google.GoogleClient;
import com.naroom.api.auth.google.GoogleUserInfo;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DirtiesContext
class GoogleLoginServiceTest {

	@Autowired
	private GoogleLoginService googleLoginService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SocialIdentityRepository socialIdentityRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private GoogleClient googleClient;

	@Test
	void newGoogleUser_createsMemberAndReturnsCompleteOnboarding() {
		String sub = String.valueOf(System.nanoTime());
		when(googleClient.verify(any())).thenReturn(googleUser(sub, "지연"));

		SocialLoginResponse response = googleLoginService.login(loginRequest("installation-new-" + sub));

		assertEquals(NextAction.COMPLETE_ONBOARDING, response.nextAction());
		assertEquals(MemberStatus.ACTIVE, response.account().status());
		assertNotNull(socialIdentityRepository
				.findByProviderAndProviderUserId(SocialProvider.GOOGLE, sub)
				.orElseThrow());
	}

	@Test
	void existingGoogleUser_reusesMemberAndReturnsEnterApp() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.GOOGLE, sub, null, false, "지연", null));
		markOnboardingCompleted(member);

		when(googleClient.verify(any())).thenReturn(googleUser(sub, "지연"));

		SocialLoginResponse response = googleLoginService.login(loginRequest("installation-existing-" + sub));

		assertEquals(NextAction.ENTER_APP, response.nextAction());
		assertEquals(member.getId(), response.account().memberId());
	}

	@Test
	void sameEmailAsExistingKakaoMember_doesNotAutoMerge() {
		String kakaoProviderUserId = String.valueOf(System.nanoTime());
		String googleSub = kakaoProviderUserId + "-google";
		Member kakaoMember = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				kakaoMember, SocialProvider.KAKAO, kakaoProviderUserId, "same@example.com", true, "지연", null));

		when(googleClient.verify(any()))
				.thenReturn(new GoogleUserInfo(googleSub, "same@example.com", true, "지연", null));

		SocialLoginResponse response = googleLoginService.login(loginRequest("installation-" + googleSub));

		assertNotEquals(kakaoMember.getId(), response.account().memberId());
	}

	@Test
	void invalidToken_throwsProviderTokenInvalid() {
		when(googleClient.verify(any())).thenThrow(new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> googleLoginService.login(loginRequest("installation-invalid")));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void revokedSocialIdentity_throwsSocialIdentityRevoked() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		SocialIdentity socialIdentity = socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.GOOGLE, sub, null, false, "지연", null));
		entityManager.createNativeQuery("update social_identities set status = 'REVOKED'::identity_status where id = :id")
				.setParameter("id", socialIdentity.getId())
				.executeUpdate();
		entityManager.clear();

		when(googleClient.verify(any())).thenReturn(googleUser(sub, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> googleLoginService.login(loginRequest("installation-revoked-" + sub)));
		assertEquals(AuthErrorCode.AUTH_SOCIAL_IDENTITY_REVOKED, exception.errorCode());
	}

	@Test
	void lockedMember_throwsAccountLocked() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.GOOGLE, sub, null, false, "지연", null));
		entityManager.createQuery("update Member m set m.status = :status where m.id = :id")
				.setParameter("status", MemberStatus.LOCKED)
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		when(googleClient.verify(any())).thenReturn(googleUser(sub, "지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> googleLoginService.login(loginRequest("installation-locked-" + sub)));
		assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.errorCode());
	}

	@Test
	void missingInstallationKey_throwsDeviceInstallationKeyRequired() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> googleLoginService.login(new GoogleLoginRequest(
						"google-id-token", new DeviceInfo("", "IOS", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED, exception.errorCode());
	}

	private void markOnboardingCompleted(Member member) {
		entityManager.createQuery("update Member m set m.onboardingCompletedAt = :now where m.id = :id")
				.setParameter("now", Instant.now())
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();
	}

	private GoogleLoginRequest loginRequest(String installationKey) {
		return new GoogleLoginRequest("google-id-token", new DeviceInfo(installationKey, "IOS", "1.0.0"));
	}

	private GoogleUserInfo googleUser(String sub, String name) {
		return new GoogleUserInfo(sub, null, false, name, null);
	}

}
