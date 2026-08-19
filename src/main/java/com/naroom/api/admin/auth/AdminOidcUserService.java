package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminStatus;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Admin Web Implementation Spec 17.1: "Google 인증"과 "Naroom 관리자 인가"는 반드시 분리된 두 단계다.
 * super.loadUser()가 서명·iss·aud·exp·nonce를 이미 검증한 뒤 여기서는 그 결과(sub)가 사전 승인된
 * admin_users와 일치하는지만 확인한다. 일치하지 않으면 이 시점에 예외를 던져 SecurityContext에
 * 인증 정보가 절대 채워지지 않게 한다 - 미승인 계정으로 admin_users row를 자동 생성하지 않는다.
 */
@Service
public class AdminOidcUserService extends OidcUserService {

	private static final String ERROR_NOT_ALLOWLISTED = "admin_not_allowlisted";
	private static final String ERROR_ACCOUNT_DISABLED = "admin_account_disabled";

	private final AdminUserRepository adminUserRepository;

	public AdminOidcUserService(AdminUserRepository adminUserRepository) {
		this.adminUserRepository = adminUserRepository;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = super.loadUser(userRequest);
		String sub = oidcUser.getSubject();

		AdminUser adminUser = adminUserRepository.findByGoogleSub(sub)
				.orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(ERROR_NOT_ALLOWLISTED)));
		if (adminUser.getStatus() != AdminStatus.ACTIVE) {
			throw new OAuth2AuthenticationException(new OAuth2Error(ERROR_ACCOUNT_DISABLED));
		}

		adminUser.recordLogin(oidcUser.getEmail(), Boolean.TRUE.equals(oidcUser.getEmailVerified()), oidcUser.getFullName());
		adminUserRepository.save(adminUser);

		return oidcUser;
	}

}
