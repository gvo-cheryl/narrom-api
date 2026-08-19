package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminSession;
import com.naroom.api.admin.domain.entity.AdminStatus;
import com.naroom.api.admin.domain.error.AdminErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * 쿠키의 관리자 세션 토큰을 검증해 SecurityContext를 채운다. JwtAuthenticationFilter와 같은 원칙으로,
 * 여기서는 예외를 던지지 않고 실패 사유를 request attribute로만 남긴다 - 공개 경로(로그인 시작 등)에
 * 낡은 쿠키가 섞여 들어와도 그 경로 자체를 막지 않기 위해서다.
 *
 * 일부러 @Component를 붙이지 않는다 - Filter 구현체를 컴포넌트 스캔에 노출하면 spring-boot-starter-oauth2-client
 * 도입 이후 무관한 @WebMvcTest 슬라이스까지 이 Filter를 자동으로 끌어와 생성자 의존성(AdminSessionService)이
 * 없다는 이유로 컨텍스트 로딩이 깨지는 걸 실제로 확인했다. AdminSecurityConfig가 직접 new로 생성해 체인에만 넣는다.
 */
public class AdminSessionAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_FAILURE_ATTRIBUTE = "adminAuthFailureErrorCode";

	private final AdminSessionService adminSessionService;
	private final AdminSessionProperties properties;

	public AdminSessionAuthenticationFilter(AdminSessionService adminSessionService, AdminSessionProperties properties) {
		this.adminSessionService = adminSessionService;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		findCookie(request).ifPresent(rawToken -> authenticate(rawToken, request));
		filterChain.doFilter(request, response);
	}

	private Optional<String> findCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return java.util.Arrays.stream(cookies)
				.filter(cookie -> properties.cookieName().equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}

	private void authenticate(String rawToken, HttpServletRequest request) {
		Optional<AdminSession> session = adminSessionService.validateAndTouch(rawToken);
		if (session.isEmpty()) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AdminErrorCode.ADMIN_SESSION_EXPIRED);
			return;
		}
		AdminSession found = session.get();
		if (found.getAdminUser().getStatus() != AdminStatus.ACTIVE) {
			request.setAttribute(AUTH_FAILURE_ATTRIBUTE, AdminErrorCode.ADMIN_ACCOUNT_DISABLED);
			return;
		}
		SecurityContextHolder.getContext().setAuthentication(new AdminAuthentication(
				found.getAdminUser().getId(), found.getId(), found.getAdminUser().getRoles()));
	}

}
