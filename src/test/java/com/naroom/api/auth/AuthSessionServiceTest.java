package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class AuthSessionServiceTest {

	@Autowired
	private AuthSessionService authSessionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Test
	void revokeBySessionId_marksSessionRevoked() {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, "installation-logout-" + System.nanoTime(), "IOS", "1.0.0"));
		IssuedTokens tokens = authSessionService.issue(member, device);

		authSessionService.revoke(tokens.session().getId(), "LOGOUT");
		authSessionRepository.flush();

		AuthSession session = authSessionRepository.findById(tokens.session().getId()).orElseThrow();
		assertNotNull(session.getRevokedAt());
		assertEquals("LOGOUT", session.getRevokeReason());
	}

	@Test
	void revokeBySessionId_sessionNotFound_throwsAuthSessionNotFound() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> authSessionService.revoke(UUID.randomUUID(), "LOGOUT"));
		assertEquals(AuthErrorCode.AUTH_SESSION_NOT_FOUND, exception.errorCode());
	}

}
