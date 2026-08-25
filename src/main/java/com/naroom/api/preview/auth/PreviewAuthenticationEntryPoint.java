package com.naroom.api.preview.auth;

import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.security.SecurityProblemWriter;
import com.naroom.api.preview.domain.error.PreviewErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PreviewAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityProblemWriter securityProblemWriter;

	public PreviewAuthenticationEntryPoint(SecurityProblemWriter securityProblemWriter) {
		this.securityProblemWriter = securityProblemWriter;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		Object failureReason = request.getAttribute(PreviewTokenAuthenticationFilter.AUTH_FAILURE_ATTRIBUTE);
		ErrorCode errorCode = (failureReason instanceof ErrorCode errorCodeAttribute)
				? errorCodeAttribute
				: PreviewErrorCode.PREVIEW_AUTHENTICATION_FAILED;
		securityProblemWriter.write(request, response, errorCode);
	}

}
