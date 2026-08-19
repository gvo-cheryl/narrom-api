package com.naroom.api.admin.auth;

import com.naroom.api.admin.audit.AdminAuditLogService;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.error.AdminErrorCode;
import com.naroom.api.global.security.SecurityProblemWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Admin Web Implementation Spec 17.4: 화면에는 "승인된 관리자 계정이 아닙니다."류의 일반 메시지만 노출하고,
// 실패 사유(ADMIN_NOT_ALLOWLISTED 등)는 감사 로그에만 내부 코드로 남긴다.
@Component
public class AdminAuthenticationFailureHandler implements AuthenticationFailureHandler {

	private final AdminAuditLogService auditLogService;
	private final SecurityProblemWriter securityProblemWriter;

	public AdminAuthenticationFailureHandler(AdminAuditLogService auditLogService, SecurityProblemWriter securityProblemWriter) {
		this.auditLogService = auditLogService;
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
		String internalReason = exception instanceof OAuth2AuthenticationException oauth2Exception
				? oauth2Exception.getError().getErrorCode()
				: exception.getClass().getSimpleName();

		auditLogService.record(
				null,
				"ADMIN_LOGIN",
				"AdminSession",
				null,
				internalReason,
				request.getHeader("X-Trace-Id"),
				request.getMethod(),
				request.getRequestURI(),
				AdminAuditOutcome.FAILURE);

		securityProblemWriter.write(request, response, AdminErrorCode.ADMIN_ACCESS_DENIED);
	}

}
