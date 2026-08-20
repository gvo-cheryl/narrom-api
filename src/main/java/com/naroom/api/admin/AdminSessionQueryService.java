package com.naroom.api.admin;

import com.naroom.api.admin.domain.entity.AdminSession;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminSessionRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.dto.AdminSessionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminSessionQueryService {

	private final AdminUserRepository adminUserRepository;
	private final AdminSessionRepository adminSessionRepository;

	public AdminSessionQueryService(AdminUserRepository adminUserRepository, AdminSessionRepository adminSessionRepository) {
		this.adminUserRepository = adminUserRepository;
		this.adminSessionRepository = adminSessionRepository;
	}

	@Transactional(readOnly = true)
	public AdminSessionResponse check(UUID adminUserId, UUID adminSessionId) {
		AdminUser adminUser = adminUserRepository.findById(adminUserId)
				.orElseThrow(() -> new IllegalStateException("AdminUser must exist for an authenticated admin session"));
		AdminSession adminSession = adminSessionRepository.findById(adminSessionId)
				.orElseThrow(() -> new IllegalStateException("AdminSession must exist for an authenticated admin session"));
		return new AdminSessionResponse(
				adminUser.getId(), adminUser.getEmail(), adminUser.getDisplayName(), adminUser.getRoles(), adminSession.getExpiresAt());
	}

}
