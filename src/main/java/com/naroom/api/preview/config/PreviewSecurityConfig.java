package com.naroom.api.preview.config;

import com.naroom.api.admin.preview.PreviewSessionService;
import com.naroom.api.preview.auth.PreviewAuthenticationEntryPoint;
import com.naroom.api.preview.auth.PreviewTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 회원 JWT 체인(SecurityConfig, @Order(2))·관리자 세션 체인(AdminSecurityConfig, @Order(1))과 완전히
 * 분리된 미리보기 전용 체인. /api/v1/preview/** 요청만 이 체인을 탄다. CSRF는 두지 않는다 - preview
 * token은 쿠키가 아니라 헤더로만 전달되어 브라우저가 자동으로 실어 보내지 않으므로 CSRF의 전제(ambient
 * 쿠키 인증)가 성립하지 않는다.
 */
@Configuration
public class PreviewSecurityConfig {

	private final PreviewSessionService previewSessionService;
	private final PreviewAuthenticationEntryPoint previewAuthenticationEntryPoint;

	public PreviewSecurityConfig(
			PreviewSessionService previewSessionService, PreviewAuthenticationEntryPoint previewAuthenticationEntryPoint) {
		this.previewSessionService = previewSessionService;
		this.previewAuthenticationEntryPoint = previewAuthenticationEntryPoint;
	}

	@Bean
	public CorsConfigurationSource previewCorsConfigurationSource(PreviewCorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "X-Preview-Token", "X-Trace-Id"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/v1/preview/**", configuration);
		return source;
	}

	@Bean
	@Order(0)
	public SecurityFilterChain previewSecurityFilterChain(
			HttpSecurity http, CorsConfigurationSource previewCorsConfigurationSource) throws Exception {
		http
				.securityMatcher("/api/v1/preview/**")
				.cors(cors -> cors.configurationSource(previewCorsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(previewAuthenticationEntryPoint))
				.addFilterBefore(
						new PreviewTokenAuthenticationFilter(previewSessionService),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
