package com.naroom.api.global.config;

import com.naroom.api.auth.security.JwtAuthenticationFilter;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// TODO: AuthenticationManager/UserDetailsService를 쓰지 않는 구조라(JwtAuthenticationFilter가 SecurityContext를
// 직접 채운다) Boot가 기동 시 임시 in-memory 사용자 비밀번호를 계속 로그에 찍는다. httpBasic/formLogin을 안 쓰므로
// 실제로는 무해하다. 거슬리면 커스텀 AuthenticationManager 빈을 등록해 UserDetailsServiceAutoConfiguration을 끌 수 있다.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/**
	 * authentication.md "공개·보호 엔드포인트" 기준. 삭제 대기(PENDING_DELETION) 복구 엔드포인트들은
	 * 세션이 이미 전부 폐기된 사용자가 호출하는 것이라 Access Token을 요구할 수 없다 - 로그인 엔드포인트와
	 * 동일하게 공개하되, provider 재인증 자체로 본인 확인을 한다.
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
			"/api/v1/auth/restore",
			"/api/v1/auth/google/login",
			"/api/v1/auth/google/restore",
			"/api/v1/auth/apple/login",
			"/api/v1/auth/apple/restore",
			"/api/v1/auth/refresh"
	};

	private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
	private final ApiAccessDeniedHandler apiAccessDeniedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(
			ApiAuthenticationEntryPoint apiAuthenticationEntryPoint,
			ApiAccessDeniedHandler apiAccessDeniedHandler,
			JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.apiAuthenticationEntryPoint = apiAuthenticationEntryPoint;
		this.apiAccessDeniedHandler = apiAccessDeniedHandler;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	// /api/v1/admin/**은 AdminSecurityConfig(@Order(1))가 먼저 가져가므로, 회원 체인은 그 나머지를 담당하는
	// catch-all로 뒤에 둔다.
	@Bean
	@Order(2)
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
						.accessDeniedHandler(apiAccessDeniedHandler))
				// ExceptionTranslationFilter보다 앞에서 실행되어야 인증 실패가 ApiAuthenticationEntryPoint로 간다.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
