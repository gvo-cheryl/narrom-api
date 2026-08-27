package com.naroom.api.admin.bootstrap;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.repository.AdminInvitationRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

// AdminBootstrapRunner(수동 CLI, --admin-bootstrap 필요)와 달리 매 기동마다 자동으로 실행된다.
// naroom.admin.super-admin-bootstrap-email이 설정돼 있고 그 이메일로 아직 admin_users도
// admin_invitations도 없을 때만 SUPER_ADMIN 초대를 새로 만든다 - 멱등적이라 재배포마다 반복 실행해도
// 안전하다. 실제 승인(admin_users 생성)은 그 이메일로 첫 Google 로그인이 성공할 때 AdminLoginResolver가
// 수행한다.
@Component
public class AdminSuperAdminInvitationSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSuperAdminInvitationSeeder.class);

	private final AdminUserRepository adminUserRepository;
	private final AdminInvitationRepository adminInvitationRepository;
	private final String superAdminBootstrapEmail;

	public AdminSuperAdminInvitationSeeder(
			AdminUserRepository adminUserRepository,
			AdminInvitationRepository adminInvitationRepository,
			@Value("${naroom.admin.super-admin-bootstrap-email:}") String superAdminBootstrapEmail) {
		this.adminUserRepository = adminUserRepository;
		this.adminInvitationRepository = adminInvitationRepository;
		this.superAdminBootstrapEmail = superAdminBootstrapEmail;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (superAdminBootstrapEmail == null || superAdminBootstrapEmail.isBlank()) {
			return;
		}
		if (adminUserRepository.existsByEmailIgnoreCase(superAdminBootstrapEmail)
				|| adminInvitationRepository.existsByEmailIgnoreCase(superAdminBootstrapEmail)) {
			return;
		}
		adminInvitationRepository.save(AdminInvitation.invite(superAdminBootstrapEmail, Set.of(AdminRole.SUPER_ADMIN), null));
		log.info("[admin-super-admin-bootstrap] created pending SUPER_ADMIN invitation");
	}

}
