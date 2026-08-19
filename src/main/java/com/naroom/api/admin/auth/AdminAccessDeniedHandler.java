package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.error.AdminErrorCode;
import com.naroom.api.global.security.SecurityProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증은 됐지만(유효한 관리자 세션) 역할이 부족해 @PreAuthorize에 막힌 경우.
@Component
public class AdminAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityProblemWriter securityProblemWriter;

	public AdminAccessDeniedHandler(SecurityProblemWriter securityProblemWriter) {
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		securityProblemWriter.write(request, response, AdminErrorCode.ADMIN_ACCESS_DENIED);
	}

}
