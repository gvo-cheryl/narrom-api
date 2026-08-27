package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.entity.AdminStatus;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminInvitationRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Google 인증(서명·iss·aud·exp·nonce 검증)과 Naroom 관리자 인가는 분리된 두 단계다(Admin Web
// Implementation Spec 17.1). 이 클래스가 인가 단계 전체를 담당한다 - AdminOidcUserService는 이 결과를
// OAuth2AuthenticationException으로만 옮겨준다. sub로 이미 등록된 관리자를 못 찾으면, 이메일이 일치하는
// 대기 중인 초대(admin_invitations)가 있는지 확인해 있으면 그 자리에서 admin_users로 전환한다 - 로그인
// 성공만으로 관리자가 자동 생성되지는 않는다(초대 자체가 명시적 승인 행위).
@Component
public class AdminLoginResolver {

	private final AdminUserRepository adminUserRepository;
	private final AdminInvitationRepository adminInvitationRepository;

	public AdminLoginResolver(AdminUserRepository adminUserRepository, AdminInvitationRepository adminInvitationRepository) {
		this.adminUserRepository = adminUserRepository;
		this.adminInvitationRepository = adminInvitationRepository;
	}

	@Transactional
	public AdminUser resolve(String googleSub, String email, boolean emailVerified, String displayName) {
		Optional<AdminUser> bySub = adminUserRepository.findByGoogleSub(googleSub);
		if (bySub.isPresent()) {
			AdminUser adminUser = bySub.get();
			if (adminUser.getStatus() != AdminStatus.ACTIVE) {
				throw new AdminLoginRejectedException(AdminLoginRejectedException.Reason.ACCOUNT_DISABLED, googleSub);
			}
			adminUser.recordLogin(email, emailVerified, displayName);
			return adminUserRepository.save(adminUser);
		}

		if (emailVerified) {
			Optional<AdminInvitation> invitation =
					adminInvitationRepository.findByEmailIgnoreCaseAndConsumedAtIsNullAndRevokedAtIsNull(email);
			if (invitation.isPresent()) {
				AdminInvitation pending = invitation.get();
				AdminUser adminUser = AdminUser.activateFromInvitation(
						googleSub, email, displayName, pending.getRoles(), pending.getInvitedByAdminId());
				adminUser = adminUserRepository.save(adminUser);
				pending.consume(adminUser.getId());
				adminInvitationRepository.save(pending);
				return adminUser;
			}
		}

		throw new AdminLoginRejectedException(AdminLoginRejectedException.Reason.NOT_ALLOWLISTED, googleSub);
	}

}
