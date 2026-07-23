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
		// TODO: 권한(role/scope) 모델이 생기면 사유별로 다른 코드가 필요할 수 있다. 지금은 인가 실패가 전부 AUTH_FORBIDDEN 하나뿐.
		securityProblemWriter.write(request, response, AuthErrorCode.AUTH_FORBIDDEN);
	}

}
