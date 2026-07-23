package com.naroom.api.global.security;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityProblemWriter securityProblemWriter;

	public ApiAccessDeniedHandler(SecurityProblemWriter securityProblemWriter) {
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		securityProblemWriter.write(request, response, AuthErrorCode.AUTH_FORBIDDEN);
	}

}
