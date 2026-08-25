package com.naroom.api.preview.auth;

import com.naroom.api.admin.domain.entity.PreviewSession;
import com.naroom.api.admin.preview.PreviewSessionService;
import com.naroom.api.preview.domain.error.PreviewErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * §16.2: preview token은 URL query에 남기지 않고 iframe handshake 후 메모리에서만 쓴다 - 쿠키가 아니라
 * 매 요청의 X-Preview-Token 헤더로 받는다. AdminSessionAuthenticationFilter와 같은 원칙으로 여기서는
 * 예외를 던지지 않고 실패 사유를 request attribute로만 남긴다.
 *
 * AdminSessionAuthenticationFilter와 같은 이유로 @Component를 붙이지 않는다 - PreviewSecurityConfig가
 * 직접 new로 생성해 체인에만 넣는다.
 */
public class PreviewTokenAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_FAILURE_ATTRIBUTE = "previewAuthFailureErrorCode";
	public static final String TOKEN_HEADER = "X-Preview-Token";

	private final PreviewSessionService previewSessionService;

	public PreviewTokenAuthenticationFilter(PreviewSessionService previewSessionService) {
		this.previewSessionService = previewSessionService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		findToken(request).ifPresent(rawToken -> authenticate(rawToken, request));
		filterChain.doFilter(request, response);
	}

	private Optional<String> findToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(TOKEN_HEADER)).filter(token -> !token.isBlank());
	}

	private void authenticate(String rawToken, HttpServletRequest request) {
		Optional<PreviewSession> session = previewSessionService.validate(rawToken);
		if (session.isEmpty()) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, PreviewErrorCode.PREVIEW_SESSION_EXPIRED);
			return;
		}
		PreviewSession found = session.get();
		SecurityContextHolder.getContext().setAuthentication(new PreviewAuthentication(
				found.getId(), previewSessionService.readSelectedContentVersions(found), found.getScenarioKey()));
	}

}
