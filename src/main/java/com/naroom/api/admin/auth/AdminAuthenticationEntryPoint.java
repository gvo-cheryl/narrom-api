package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.error.AdminErrorCode;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.security.SecurityProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter securityProblemWriter;

	public AdminAuthenticationEntryPoint(SecurityProblemWriter securityProblemWriter) {
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		Object failureReason = request.getAttribute(AdminSessionAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE);
		ErrorCode errorCode = (failureReason instanceof ErrorCode errorCodeAttribute)
				? errorCodeAttribute
				: AdminErrorCode.ADMIN_AUTHENTICATION_FAILED;
		securityProblemWriter.write(request, response, errorCode);
	}

}
