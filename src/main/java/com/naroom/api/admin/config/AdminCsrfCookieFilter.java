package com.naroom.api.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Spring Security 6의 CsrfToken은 지연 로딩이라 아무도 값을 읽지 않으면 CookieCsrfTokenRepository가
// XSRF-TOKEN 쿠키를 절대 내려주지 않는다(공식 CSRF 문서의 SPA 연동 가이드와 동일한 문제). naroom-admin이
// 매 요청마다 새로 토큰을 읽을 수 있도록 요청 attribute에서 강제로 한 번 resolve한다.
@Component
public class AdminCsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (csrfToken != null) {
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}

}
