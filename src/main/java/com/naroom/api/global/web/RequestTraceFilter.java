package com.naroom.api.global.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Spring Security의 Filter Chain(기본 order -100)보다 먼저 실행되어야
 * 인증 실패로 조기 종료되는 요청에도 traceId가 항상 존재한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

	public static final String TRACE_ID_ATTRIBUTE = "traceId";
	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String traceId = UUID.randomUUID().toString();
		request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
		response.setHeader(TRACE_ID_HEADER, traceId);
		MDC.put(TRACE_ID_ATTRIBUTE, traceId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(TRACE_ID_ATTRIBUTE);
		}
	}

}
