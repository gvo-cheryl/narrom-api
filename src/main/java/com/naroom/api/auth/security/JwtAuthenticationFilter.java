package com.naroom.api.auth.security;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * Authorization 헤더가 있으면 검증하고, 없으면 그냥 통과시킨다(익명 요청으로 취급).
 * 실패해도 여기서 예외를 던지지 않는다 — 이 필터는 SecurityConfig의 permitAll 여부 판단(AuthorizationFilter)보다
 * 먼저 실행되므로, 여기서 던지면 공개 경로(health 등)에 낡은 토큰 하나 잘못 들어와도 막혀버린다.
 * 대신 실패 사유를 request attribute로 남기고, 실제로 인증이 필요한 경로에서만
 * ApiAuthenticationEntryPoint가 그 사유를 읽어 구체적인 코드(만료/위조 등)로 응답한다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_FAILURE_ATTRIBUTE = "authFailureErrorCode";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final AuthSessionRepository authSessionRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthSessionRepository authSessionRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.authSessionRepository = authSessionRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			authenticate(header.substring(BEARER_PREFIX.length()), request);
		}
		filterChain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		AccessTokenClaims claims;
		try {
			claims = jwtTokenProvider.parse(token);
		} catch (ExpiredJwtException e) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
			return;
		} catch (JwtException | IllegalArgumentException e) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID);
			return;
		}

		Optional<AuthSession> session = authSessionRepository.findById(claims.sessionId());
		if (session.isEmpty()) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_SESSION_NOT_FOUND);
			return;
		}
		if (!session.get().getMember().getId().equals(claims.memberId())) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_ACCESS_TOKEN_INVALID);
			return;
		}
		if (session.get().getRevokedAt() != null) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_SESSION_REVOKED);
			return;
		}
		if (session.get().getExpiresAt().isBefore(Instant.now())) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AuthErrorCode.AUTH_SESSION_EXPIRED);
			return;
		}

		SecurityContextHolder.getContext()
				.setAuthentication(new MemberAuthentication(claims.memberId(), claims.sessionId()));
	}

}
