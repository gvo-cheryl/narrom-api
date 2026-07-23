package com.naroom.api.global.config;

import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// TODO: 아직 UserDetailsService/AuthenticationProvider가 없어 Boot가 기동 시 임시 in-memory
// 사용자 비밀번호를 로그에 찍는다. httpBasic/formLogin을 안 쓰므로 실제로는 무해하지만,
// JWT 인증(오늘 10번)이 들어오면 이 경고는 자연히 사라진다.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * authentication.md "공개·보호 엔드포인트" 기준. 카카오 로그인·재발급·계정 복구는
	 * 아직 Controller가 없지만(오늘 12번 구현 예정) 계약상 공개 경로로 이미 확정되어 있어 미리 등록한다.
	 */
	private static final String[] PUBLIC_PATHS = {
			"/api/v1/health",
			"/actuator/**",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs",
			"/v3/api-docs.yaml",
			"/v3/api-docs/**",
			"/api/v1/auth/kakao/login",
			"/api/v1/auth/kakao/account-recovery",
			"/api/v1/auth/refresh"
	};

	private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
	private final ApiAccessDeniedHandler apiAccessDeniedHandler;

	public SecurityConfig(
			ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
			ApiAccessDeniedHandler apiAccessDeniedHandler) {
		this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
		this.apiAccessDeniedHandler = apiAccessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(apiAuthenticationEntryPoint)
						.accessDeniedHandler(apiAccessDeniedHandler));

		return http.build();
	}

}
