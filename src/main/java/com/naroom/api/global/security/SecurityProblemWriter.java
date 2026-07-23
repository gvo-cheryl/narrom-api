package com.naroom.api.global.security;

import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Security Filter Chain에서 발생한 인증·인가 실패를 {@code GlobalExceptionHandler}와
 * 동일한 ProblemDetail 형식으로 직접 응답에 기록한다. {@code @RestControllerAdvice}는
 * 이 경로를 처리하지 못하기 때문에 별도로 둔다.
 */
@Component
public class SecurityProblemWriter {

	private final ProblemDetailFactory problemDetailFactory;
	private final ObjectMapper objectMapper;

	public SecurityProblemWriter(ProblemDetailFactory problemDetailFactory, ObjectMapper objectMapper) {
		this.problemDetailFactory = problemDetailFactory;
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
		ProblemDetail problemDetail = problemDetailFactory.create(errorCode, request);
		response.setStatus(errorCode.httpStatus().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
	}

}
