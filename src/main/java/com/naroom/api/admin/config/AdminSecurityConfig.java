package com.naroom.api.admin.config;

import com.naroom.api.admin.auth.AdminAccessDeniedHandler;
import com.naroom.api.admin.auth.AdminAuthenticationEntryPoint;
import com.naroom.api.admin.auth.AdminAuthenticationFailureHandler;
import com.naroom.api.admin.auth.AdminAuthenticationSuccessHandler;
import com.naroom.api.admin.auth.AdminOidcUserService;
import com.naroom.api.admin.auth.AdminSessionAuthenticationFilter;
import com.naroom.api.admin.auth.AdminSessionProperties;
import com.naroom.api.admin.auth.AdminSessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 회원 JWT 체인(SecurityConfig)과 완전히 분리된 관리자 전용 체인. /api/v1/admin/** 요청만 이 체인을
 * 탄다 - 두 체인 모두 securityMatcher가 있어야 하므로 SecurityConfig 쪽에도 @Order(2)를 붙여둔다.
 * OAuth2 Login은 code 교환·ID Token 검증까지만 담당하고(AdminOidcUserService), 성공 이후의 실제 세션은
 * AdminAuthenticationSuccessHandler가 admin_sessions 쿠키로 전환한다.
 *
 * ClientRegistrationRepository를 spring.security.oauth2.client.registration.* YAML 대신 여기서 직접
 * 만든다 - Spring Boot의 OAuth2ClientAutoConfiguration(YAML 기반)이 활성화되면 SecurityFilterChain을
 * 전부 즉시 인스턴스화하려 들어서, HttpSecurity/AdminSessionService 등을 못 구하는 무관한 @WebMvcTest
 * 슬라이스가 전부 깨지는 걸 실제로 겪었다. 직접 빈을 등록하면(@ConditionalOnMissingBean) Boot의 자동
 * 설정이 스스로 물러나 이 문제가 사라진다.
 */
@Configuration
@EnableMethodSecurity
public class AdminSecurityConfig {

	private final AdminOidcUserService adminOidcUserService;
	private final AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler;
	private final AdminAuthenticationFailureHandler adminAuthenticationFailureHandler;
	private final AdminSessionService adminSessionService;
	private final AdminSessionProperties adminSessionProperties;
	private final AdminAuthenticationEntryPoint adminAuthenticationEntryPoint;
	private final AdminAccessDeniedHandler adminAccessDeniedHandler;

	public AdminSecurityConfig(
			AdminOidcUserService adminOidcUserService,
			AdminAuthenticationSuccessHandler adminAuthenticationSuccessHandler,
			AdminAuthenticationFailureHandler adminAuthenticationFailureHandler,
			AdminSessionService adminSessionService,
			AdminSessionProperties adminSessionProperties,
			AdminAuthenticationEntryPoint adminAuthenticationEntryPoint,
			AdminAccessDeniedHandler adminAccessDeniedHandler) {
		this.adminOidcUserService = adminOidcUserService;
		this.adminAuthenticationSuccessHandler = adminAuthenticationSuccessHandler;
		this.adminAuthenticationFailureHandler = adminAuthenticationFailureHandler;
		this.adminSessionService = adminSessionService;
		this.adminSessionProperties = adminSessionProperties;
		this.adminAuthenticationEntryPoint = adminAuthenticationEntryPoint;
		this.adminAccessDeniedHandler = adminAccessDeniedHandler;
	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(AdminGoogleOAuthProperties properties) {
		ClientRegistration google = ClientRegistration.withRegistrationId("google")
				.clientId(properties.clientId())
				.clientSecret(properties.clientSecret())
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/api/v1/admin/auth/callback/{registrationId}")
				.scope("openid", "email", "profile")
				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
				.tokenUri("https://www.googleapis.com/oauth2/v4/token")
				.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
				.issuerUri("https://accounts.google.com")
				.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
				.userNameAttributeName(IdTokenClaimNames.SUB)
				.clientName("Google")
				.build();
		return new InMemoryClientRegistrationRepository(google);
	}

	@Bean
	@Order(1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/api/v1/admin/**")
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				// OAuth2 Login의 authorization request(state/nonce/PKCE)는 콜백까지의 짧은 구간만 HttpSession이
				// 필요하다 - 로그인 이후 실제 요청 인증은 AdminSessionAuthenticationFilter의 자체 쿠키가 담당한다.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/v1/admin/auth/login/**", "/api/v1/admin/auth/callback/**").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(a -> a.baseUri("/api/v1/admin/auth/login"))
						.redirectionEndpoint(r -> r.baseUri("/api/v1/admin/auth/callback/*"))
						.userInfoEndpoint(u -> u.oidcUserService(adminOidcUserService))
						.successHandler(adminAuthenticationSuccessHandler)
						.failureHandler(adminAuthenticationFailureHandler))
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint(adminAuthenticationEntryPoint)
						.accessDeniedHandler(adminAccessDeniedHandler))
				.addFilterBefore(
						new AdminSessionAuthenticationFilter(adminSessionService, adminSessionProperties),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
