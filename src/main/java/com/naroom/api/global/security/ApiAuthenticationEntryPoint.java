package com.naroom.api.global.security;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.security.JwtAuthenticationFilter;
import com.naroom.api.global.error.code.ErrorCode;
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
		// JwtAuthenticationFilter가 토큰을 시도했다가 실패한 구체적 사유(만료/위조 등)가 있으면 그걸 쓰고,
		// 없으면(토큰 자체가 없었던 경우) 기본값인 AUTH_REQUIRED를 쓴다.
		Object failureReason = request.getAttribute(JwtAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE);
		ErrorCode errorCode = (failureReason instanceof ErrorCode errorCodeAttribute)
				? errorCodeAttribute
				: AuthErrorCode.AUTH_REQUIRED;
		securityProblemWriter.write(request, response, errorCode);
	}

}
