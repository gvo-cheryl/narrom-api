package com.naroom.api.global.security;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.exception.ApiAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter securityProblemWriter;

	public ApiAuthenticationEntryPoint(SecurityProblemWriter securityProblemWriter) {
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		// 아직 JWT 필터가 없어 지금은 항상 이 fallback(AUTH_REQUIRED)만 탄다.
		// 필터가 ApiAuthenticationException으로 구체적인 사유(만료/위조 등)를 던지기 시작하면 그 코드를 그대로 쓴다.
		ErrorCode errorCode = (authException instanceof ApiAuthenticationException apiAuthenticationException)
				? apiAuthenticationException.errorCode()
				: AuthErrorCode.AUTH_REQUIRED;
		securityProblemWriter.write(request, response, errorCode);
	}

}
