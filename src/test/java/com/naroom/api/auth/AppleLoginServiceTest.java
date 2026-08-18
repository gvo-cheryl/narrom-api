package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.auth.apple.AppleClient;
import com.naroom.api.auth.apple.AppleUserInfo;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.AppleLoginRequest;
import com.naroom.api.auth.dto.DeviceInfo;
import com.naroom.api.auth.dto.SocialLoginResponse;
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
class AppleLoginServiceTest {

	@Autowired
	private AppleLoginService appleLoginService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private SocialIdentityRepository socialIdentityRepository;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private AppleClient appleClient;

	@Test
	void newAppleUser_createsMemberWithFullNameFromRequest_andReturnsCompleteOnboarding() {
		String sub = String.valueOf(System.nanoTime());
		when(appleClient.verify(any(), any())).thenReturn(appleUser(sub));

		SocialLoginResponse response = appleLoginService.login(loginRequest(sub, "installation-new-" + sub, "지연"));

		assertEquals(NextAction.COMPLETE_ONBOARDING, response.nextAction());
		assertEquals(MemberStatus.ACTIVE, response.account().status());
		assertEquals("지연", response.account().displayName());
		assertNotNull(socialIdentityRepository
				.findByProviderAndProviderUserId(SocialProvider.APPLE, sub)
				.orElseThrow());
	}

	@Test
	void existingAppleUser_withoutFullNameOnReLogin_reusesMemberAndReturnsEnterApp() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.APPLE, sub, null, false, "지연", null));
		markOnboardingCompleted(member);

		when(appleClient.verify(any(), any())).thenReturn(appleUser(sub));

		SocialLoginResponse response = appleLoginService.login(loginRequest(sub, "installation-existing-" + sub, null));

		assertEquals(NextAction.ENTER_APP, response.nextAction());
		assertEquals(member.getId(), response.account().memberId());
	}

	@Test
	void privateRelayEmail_treatedAsNormalEmail() {
		String sub = String.valueOf(System.nanoTime());
		when(appleClient.verify(any(), any()))
				.thenReturn(new AppleUserInfo(sub, "abc123@privaterelay.appleid.com", true));

		SocialLoginResponse response = appleLoginService.login(loginRequest(sub, "installation-" + sub, "지연"));

		SocialIdentity identity = socialIdentityRepository
				.findByProviderAndProviderUserId(SocialProvider.APPLE, sub)
				.orElseThrow();
		assertEquals("abc123@privaterelay.appleid.com", identity.getEmail());
		assertEquals(MemberStatus.ACTIVE, response.account().status());
	}

	@Test
	void invalidNonce_throwsProviderTokenInvalid() {
		when(appleClient.verify(any(), any())).thenThrow(new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> appleLoginService.login(loginRequest("sub-1", "installation-invalid", null)));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void revokedSocialIdentity_throwsSocialIdentityRevoked() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		SocialIdentity socialIdentity = socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.APPLE, sub, null, false, "지연", null));
		entityManager.createNativeQuery("update social_identities set status = 'REVOKED'::identity_status where id = :id")
				.setParameter("id", socialIdentity.getId())
				.executeUpdate();
		entityManager.clear();

		when(appleClient.verify(any(), any())).thenReturn(appleUser(sub));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> appleLoginService.login(loginRequest(sub, "installation-revoked-" + sub, null)));
		assertEquals(AuthErrorCode.AUTH_SOCIAL_IDENTITY_REVOKED, exception.errorCode());
	}

	@Test
	void restore_pendingDeletionMember_restoresAndReturnsSession() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.APPLE, sub, null, false, "지연", null));
		entityManager.createQuery(
						"update Member m set m.status = :status, m.scheduledDeletionAt = :scheduledDeletionAt where m.id = :id")
				.setParameter("status", MemberStatus.PENDING_DELETION)
				.setParameter("scheduledDeletionAt", Instant.now().plusSeconds(604_800))
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		when(appleClient.verify(any(), any())).thenReturn(appleUser(sub));

		SocialLoginResponse response = appleLoginService.restore(loginRequest(sub, "installation-restore-" + sub, null));

		assertEquals(MemberStatus.ACTIVE, response.account().status());
		assertNotNull(response.accessToken());
		Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
		assertEquals(MemberStatus.ACTIVE, reloaded.getStatus());
	}

	@Test
	void restore_activeMember_throwsAccountNotPendingDeletion() {
		String sub = String.valueOf(System.nanoTime());
		Member member = memberRepository.save(Member.create("지연"));
		socialIdentityRepository.save(SocialIdentity.connect(
				member, SocialProvider.APPLE, sub, null, false, "지연", null));

		when(appleClient.verify(any(), any())).thenReturn(appleUser(sub));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> appleLoginService.restore(loginRequest(sub, "installation-restore-active-" + sub, null)));
		assertEquals(AuthErrorCode.ACCOUNT_NOT_PENDING_DELETION, exception.errorCode());
	}

	@Test
	void missingInstallationKey_throwsDeviceInstallationKeyRequired() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> appleLoginService.login(new AppleLoginRequest(
						"apple-identity-token", "raw-nonce", "지연", new DeviceInfo("", "IOS", "1.0.0"))));
		assertEquals(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED, exception.errorCode());
	}

	private void markOnboardingCompleted(Member member) {
		entityManager.createQuery("update Member m set m.onboardingCompletedAt = :now where m.id = :id")
				.setParameter("now", Instant.now())
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();
	}

	private AppleLoginRequest loginRequest(String sub, String installationKey, String fullName) {
		return new AppleLoginRequest(
				"apple-identity-token-" + sub, "raw-nonce", fullName, new DeviceInfo(installationKey, "IOS", "1.0.0"));
	}

	private AppleUserInfo appleUser(String sub) {
		return new AppleUserInfo(sub, null, false);
	}

}
