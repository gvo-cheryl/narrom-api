package com.naroom.api.admin.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Admin Web Implementation Spec 17.1: "Google 인증"과 "Naroom 관리자 인가"는 반드시 분리된 두 단계다.
 * super.loadUser()가 서명·iss·aud·exp·nonce를 이미 검증한 뒤, 실제 인가 판단은 AdminLoginResolver에
 * 위임한다(OidcUserRequest를 직접 다루지 않는 순수 로직이라 단위 테스트가 쉽다).
 */
@Service
public class AdminOidcUserService extends OidcUserService {

	private final AdminLoginResolver adminLoginResolver;

	public AdminOidcUserService(AdminLoginResolver adminLoginResolver) {
		this.adminLoginResolver = adminLoginResolver;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);
		try {
			adminLoginResolver.resolve(
					oidcUser.getSubject(),
					oidcUser.getEmail(),
					Boolean.TRUE.equals(oidcUser.getEmailVerified()),
					oidcUser.getFullName());
		} catch (AdminLoginRejectedException e) {
			// OAuth2Error.description을 sub 전달용으로 쓴다 - AdminAuthenticationFailureHandler가 이 값을
			// 감사 로그에만 기록하고(브라우저 응답에는 노출하지 않음) 관리자 초대 시 sub 확인 용도로 쓴다.
			throw new OAuth2AuthenticationException(new OAuth2Error(e.reason().errorCode(), e.googleSub(), null));
		}
		return oidcUser;
	}

}
