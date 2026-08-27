package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminStatus;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminInvitationRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class AdminLoginResolverTest {

	@Autowired
	private AdminLoginResolver adminLoginResolver;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminInvitationRepository adminInvitationRepository;

	@Test
	void resolve_existingActiveAdminBySub_recordsLoginAndSucceeds() {
		AdminUser existing = adminUserRepository.save(
				AdminUser.bootstrap("sub-" + System.nanoTime(), "old@naroom.io", "이전", Set.of(AdminRole.SUPER_ADMIN)));

		AdminUser resolved = adminLoginResolver.resolve(existing.getGoogleSub(), "new@naroom.io", true, "새 이름");

		assertEquals(existing.getId(), resolved.getId());
		assertEquals("new@naroom.io", resolved.getEmail());
		assertNotNull(resolved.getLastLoginAt());
	}

	@Test
	void resolve_existingDisabledAdminBySub_throwsAccountDisabled() {
		AdminUser existing = adminUserRepository.save(
				AdminUser.bootstrap("sub-" + System.nanoTime(), "disabled@naroom.io", "비활성", Set.of(AdminRole.SUPER_ADMIN)));
		existing.disable();
		adminUserRepository.save(existing);

		AdminLoginRejectedException exception = assertThrows(
				AdminLoginRejectedException.class,
				() -> adminLoginResolver.resolve(existing.getGoogleSub(), "disabled@naroom.io", true, "비활성"));
		assertEquals(AdminLoginRejectedException.Reason.ACCOUNT_DISABLED, exception.reason());
	}

	@Test
	void resolve_pendingInvitationMatchesVerifiedEmail_activatesAndConsumesInvitation() {
		String email = "invited-" + System.nanoTime() + "@naroom.io";
		AdminUser inviter = adminUserRepository.save(
				AdminUser.bootstrap("inviter-" + System.nanoTime(), "inviter@naroom.io", "초대자", Set.of(AdminRole.SUPER_ADMIN)));
		AdminInvitation invitation = adminInvitationRepository.save(
				AdminInvitation.invite(email, Set.of(AdminRole.CONTENT_EDITOR), inviter.getId()));

		AdminUser resolved = adminLoginResolver.resolve("new-sub-" + System.nanoTime(), email, true, "신규 관리자");
		// AdminInvitation.roles 컬렉션 인스턴스를 그대로 재사용하면 flush 시점에야 Hibernate가
		// "Found shared references to a collection"으로 거부한다 - 저장만으로는 드러나지 않는다.
		adminUserRepository.flush();

		assertEquals(Set.of(AdminRole.CONTENT_EDITOR), resolved.getRoles());
		assertEquals(inviter.getId(), resolved.getApprovedByAdminId());
		assertNotNull(resolved.getId());

		AdminInvitation consumed = adminInvitationRepository.findById(invitation.getId()).orElseThrow();
		assertFalse(consumed.isPending());
		assertEquals(resolved.getId(), consumed.getConsumedAdminUserId());
	}

	@Test
	void resolve_pendingInvitationButEmailNotVerified_throwsNotAllowlisted() {
		String email = "unverified-" + System.nanoTime() + "@naroom.io";
		adminInvitationRepository.save(AdminInvitation.invite(email, Set.of(AdminRole.CONTENT_EDITOR), null));

		AdminLoginRejectedException exception = assertThrows(
				AdminLoginRejectedException.class,
				() -> adminLoginResolver.resolve("sub-" + System.nanoTime(), email, false, "미검증"));
		assertEquals(AdminLoginRejectedException.Reason.NOT_ALLOWLISTED, exception.reason());
	}

	@Test
	void resolve_noSubMatchAndNoInvitation_throwsNotAllowlisted() {
		AdminLoginRejectedException exception = assertThrows(
				AdminLoginRejectedException.class,
				() -> adminLoginResolver.resolve(
						"unknown-sub-" + System.nanoTime(), "unknown-" + System.nanoTime() + "@naroom.io", true, "미승인"));
		assertEquals(AdminLoginRejectedException.Reason.NOT_ALLOWLISTED, exception.reason());
	}

	@Test
	void resolve_revokedInvitation_doesNotActivateAndThrowsNotAllowlisted() {
		String email = "revoked-" + System.nanoTime() + "@naroom.io";
		AdminInvitation invitation = adminInvitationRepository.save(
				AdminInvitation.invite(email, Set.of(AdminRole.CONTENT_EDITOR), null));
		invitation.revoke();
		adminInvitationRepository.save(invitation);

		assertThrows(
				AdminLoginRejectedException.class,
				() -> adminLoginResolver.resolve("sub-" + System.nanoTime(), email, true, "취소됨"));
		assertTrue(adminUserRepository.existsByEmailIgnoreCase(email) == false);
	}

}
