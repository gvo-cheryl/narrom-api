package com.naroom.api.auth.security;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.auth.AuthSessionService;
import com.naroom.api.auth.IssuedTokens;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@Transactional
@DirtiesContext
class JwtAuthenticationFilterTest {

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private AuthSessionService authSessionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private AuthSessionRepository authSessionRepository;

	@Test
	void validAccessToken_setsMemberAuthentication() throws Exception {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, "installation-key-valid", "IOS", "1.0.0"));
		IssuedTokens tokens = authSessionService.issue(member, device);

		runFilter(bearer(tokens.accessToken()));

		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		assertEquals(member.getId(), authentication.getMemberId());
		assertEquals(tokens.session().getId(), authentication.getSessionId());
	}

	@Test
	void noAuthorizationHeader_leavesRequestAnonymous() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");

		runFilter(request);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertNull(request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE));
	}

	@Test
	void malformedToken_doesNotBlockTheRequest_butRecordsFailureReason() throws Exception {
		MockHttpServletRequest request = bearer("this.is.garbage");

		// 핵심 검증: 공개 경로에서 잘못된 토큰이 와도 필터 자체는 요청을 막지 않는다(예외를 던지지 않는다).
		runFilter(request);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID, request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE));
	}

	@Test
	void expiredAccessToken_recordsExpiredFailureReason() throws Exception {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, "installation-key-expired", "IOS", "1.0.0"));
		IssuedTokens tokens = authSessionService.issue(member, device);

		JwtProperties expiredProperties = new JwtProperties(
				jwtProperties.secret(), -1_000L, jwtProperties.refreshTokenExpiration(),
				jwtProperties.issuer(), jwtProperties.audience());
		String expiredToken = new JwtTokenProvider(expiredProperties)
				.issueAccessToken(member.getId(), tokens.session().getId());

		MockHttpServletRequest request = bearer(expiredToken);
		runFilter(request);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED, request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE));
	}

	@Test
	void revokedSession_recordsRevokedFailureReason() throws Exception {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device = deviceInstallationRepository.save(
				DeviceInstallation.register(member, "installation-key-revoked", "IOS", "1.0.0"));
		IssuedTokens tokens = authSessionService.issue(member, device);
		authSessionService.revoke(tokens.session(), "LOGOUT");
		authSessionRepository.flush();

		MockHttpServletRequest request = bearer(tokens.accessToken());
		runFilter(request);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		assertEquals(AuthErrorCode.AUTH_SESSION_REVOKED, request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE));
	}

	private MockHttpServletRequest bearer(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/example");
		request.addHeader("Authorization", "Bearer " + token);
		return request;
	}

	private void runFilter(MockHttpServletRequest request) throws Exception {
		SecurityContextHolder.clearContext();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = new MockFilterChain();
		jwtAuthenticationFilter.doFilter(request, response, chain);
	}

}
