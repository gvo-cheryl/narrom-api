package com.naroom.api.admin.auth;

import com.naroom.api.admin.audit.AdminAuditLogService;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.auth.security.RefreshTokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Spring Security의 OAuth2 Login은 code 교환·ID Token 검증(AdminOidcUserService까지)만 담당한다.
 * 여기서부터는 그 결과를 우리 자체 관리자 세션(admin_sessions, opaque 쿠키)으로 전환한다 -
 * Spring의 기본 HttpSession 기반 SecurityContext는 idle/absolute timeout·revoke를 우리가 원하는 방식으로
 * 제어할 수 없어서 쓰지 않는다.
 */
@Component
public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final AdminUserRepository adminUserRepository;
	private final AdminSessionService adminSessionService;
	private final AdminSessionProperties sessionProperties;
	private final AdminAuditLogService auditLogService;
	private final RefreshTokenGenerator hasher;

	public AdminAuthenticationSuccessHandler(
			AdminUserRepository adminUserRepository,
			AdminSessionService adminSessionService,
			AdminSessionProperties sessionProperties,
			AdminAuditLogService auditLogService,
			RefreshTokenGenerator hasher) {
		this.adminUserRepository = adminUserRepository;
		this.adminSessionService = adminSessionService;
		this.sessionProperties = sessionProperties;
		this.auditLogService = auditLogService;
		this.hasher = hasher;
	}

	@Override
	@Transactional
	public void onAuthenticationSuccess(
			HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
		OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
		AdminUser adminUser = adminUserRepository.findByGoogleSub(oidcUser.getSubject())
				.orElseThrow(() -> new IllegalStateException("AdminUser must exist after AdminOidcUserService validation"));

		String ipHash = request.getRemoteAddr() == null ? null : hasher.hash(request.getRemoteAddr());
		String userAgentSummary = summarize(request.getHeader(HttpHeaders.USER_AGENT));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, ipHash, userAgentSummary);

		ResponseCookie cookie = ResponseCookie.from(sessionProperties.cookieName(), issued.rawToken())
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")
				.path("/")
				.domain(blankToNull(sessionProperties.cookieDomain()))
				.maxAge(sessionProperties.absoluteTimeout())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		auditLogService.record(
				adminUser.getId(),
				"ADMIN_LOGIN",
				"AdminSession",
				issued.session().getId().toString(),
				null,
				traceIdOf(request),
				request.getMethod(),
				request.getRequestURI(),
				AdminAuditOutcome.SUCCESS);

		// 이 세션부터는 우리 쿠키(AdminSessionAuthenticationFilter)만 신뢰한다.
		SecurityContextHolder.clearContext();

		String redirectUri = sessionProperties.frontendRedirectUri();
		response.sendRedirect(redirectUri == null || redirectUri.isBlank() ? "/api/v1/admin/auth/session" : redirectUri);
	}

	private static String summarize(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static String traceIdOf(HttpServletRequest request) {
		return request.getHeader("X-Trace-Id");
	}

}
