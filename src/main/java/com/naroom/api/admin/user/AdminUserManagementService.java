package com.naroom.api.admin.user;

import com.naroom.api.admin.domain.error.AdminErrorCode;
import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.repository.AdminInvitationRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.user.dto.AdminInvitationCreateRequest;
import com.naroom.api.admin.user.dto.AdminInvitationResponse;
import com.naroom.api.admin.user.dto.AdminUserResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 새 관리자를 이메일로 미리 초대해두면(admin_invitations), 첫 Google 로그인에서 AdminLoginResolver가
// 그 초대를 admin_users로 전환한다 - google_sub를 미리 알아낼 필요가 없다.
@Service
public class AdminUserManagementService {

	private final AdminUserRepository adminUserRepository;
	private final AdminInvitationRepository adminInvitationRepository;

	public AdminUserManagementService(
			AdminUserRepository adminUserRepository, AdminInvitationRepository adminInvitationRepository) {
		this.adminUserRepository = adminUserRepository;
		this.adminInvitationRepository = adminInvitationRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminUserResponse> listAdminUsers() {
		return adminUserRepository.findAllByOrderByCreatedAtDesc().stream().map(AdminUserResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<AdminInvitationResponse> listInvitations() {
		return adminInvitationRepository.findAllByOrderByInvitedAtDesc().stream()
				.map(AdminInvitationResponse::from)
				.toList();
	}

	@Transactional
	public AdminInvitationResponse createInvitation(AdminInvitationCreateRequest request, UUID invitedByAdminId) {
		String email = request.email().trim();
		boolean pendingInvitationExists =
				adminInvitationRepository.findByEmailIgnoreCaseAndConsumedAtIsNullAndRevokedAtIsNull(email).isPresent();
		if (pendingInvitationExists || adminUserRepository.existsByEmailIgnoreCase(email)) {
			throw new BusinessException(AdminErrorCode.ADMIN_INVITATION_ALREADY_EXISTS);
		}
		AdminInvitation invitation = AdminInvitation.invite(email, request.roles(), invitedByAdminId);
		return AdminInvitationResponse.from(adminInvitationRepository.save(invitation));
	}

	@Transactional
	public AdminInvitationResponse revokeInvitation(UUID id) {
		AdminInvitation invitation = adminInvitationRepository.findById(id)
				.orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_INVITATION_NOT_FOUND));
		if (!invitation.isPending()) {
			throw new BusinessException(AdminErrorCode.ADMIN_INVITATION_NOT_PENDING);
		}
		invitation.revoke();
		return AdminInvitationResponse.from(invitation);
	}

}
