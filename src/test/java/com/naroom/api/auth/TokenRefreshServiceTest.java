package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.RefreshRequest;
import com.naroom.api.auth.dto.RefreshResponse;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class TokenRefreshServiceTest {

	@Autowired
	private TokenRefreshService tokenRefreshService;

	@Autowired
	private AuthSessionService authSessionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void validRefresh_rotatesTokenAndKeepsSameSession() {
		String installationKey = "installation-refresh-" + System.nanoTime();
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));
		IssuedTokens issued = authSessionService.issue(member, device);

		RefreshResponse response = tokenRefreshService.refresh(
				new RefreshRequest(issued.refreshToken(), installationKey));

		// accessToken은 검증하지 않는다: iat/exp가 초 단위라 같은 초에 재발급되면 이전 값과 바이트까지 동일할 수 있다.
		assertEquals(issued.session().getId(), response.session().id());
		assertNotEquals(issued.refreshToken(), response.refreshToken());
	}

	@Test
	void invalidRefreshToken_throwsAuthRefreshTokenInvalid() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest("not-a-real-token", "installation-key")));
		assertEquals(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void deviceMismatch_throwsAuthDeviceMismatch() {
		String installationKey = "installation-mismatch-" + System.nanoTime();
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));
		IssuedTokens issued = authSessionService.issue(member, device);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest(issued.refreshToken(), "different-installation")));
		assertEquals(AuthErrorCode.AUTH_DEVICE_MISMATCH, exception.errorCode());
	}

	@Test
	void revokedSession_throwsAuthSessionRevoked() {
		String installationKey = "installation-revoked-" + System.nanoTime();
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));
		IssuedTokens issued = authSessionService.issue(member, device);
		authSessionService.revoke(issued.session(), "LOGOUT");
		authSessionRepository.flush();

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest(issued.refreshToken(), installationKey)));
		assertEquals(AuthErrorCode.AUTH_SESSION_REVOKED, exception.errorCode());
	}

	@Test
	void lockedMember_throwsAccountLocked() {
		String installationKey = "installation-locked-" + System.nanoTime();
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));
		IssuedTokens issued = authSessionService.issue(member, device);
		updateMemberStatus(member, MemberStatus.LOCKED);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest(issued.refreshToken(), installationKey)));
		assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.errorCode());
	}

	@Test
	void pendingDeletionMember_throwsAccountPendingDeletion() {
		String installationKey = "installation-pending-" + System.nanoTime();
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, installationKey, "IOS", "1.0.0"));
		IssuedTokens issued = authSessionService.issue(member, device);
		entityManager.createQuery(
						"update Member m set m.status = :status, m.scheduledDeletionAt = :scheduledDeletionAt where m.id = :id")
				.setParameter("status", MemberStatus.PENDING_DELETION)
				.setParameter("scheduledDeletionAt", Instant.now().plusSeconds(604_800))
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest(issued.refreshToken(), installationKey)));
		assertEquals(AuthErrorCode.ACCOUNT_PENDING_DELETION, exception.errorCode());
	}

	@Test
	void missingInstallationKey_throwsDeviceInstallationKeyRequired() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tokenRefreshService.refresh(new RefreshRequest("some-refresh-token", "")));
		assertEquals(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED, exception.errorCode());
	}

	private void updateMemberStatus(Member member, MemberStatus status) {
		entityManager.createQuery("update Member m set m.status = :status where m.id = :id")
				.setParameter("status", status)
				.setParameter("id", member.getId())
				.executeUpdate();
		entityManager.clear();
	}

}
