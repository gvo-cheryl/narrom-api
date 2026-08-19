package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminSession;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminSessionRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class AdminSessionServiceTest {

	@Autowired
	private AdminSessionService adminSessionService;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionRepository adminSessionRepository;

	@Test
	void issue_thenValidateAndTouch_returnsSameSession() {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN)));

		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		adminSessionRepository.flush();

		Optional<AdminSession> validated = adminSessionService.validateAndTouch(issued.rawToken());

		assertTrue(validated.isPresent());
		assertEquals(issued.session().getId(), validated.get().getId());
	}

	@Test
	void validateAndTouch_unknownToken_returnsEmpty() {
		Optional<AdminSession> validated = adminSessionService.validateAndTouch("never-issued-token");

		assertTrue(validated.isEmpty());
	}

	@Test
	void validateAndTouch_revokedSession_returnsEmpty() {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN)));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");

		adminSessionService.revoke(issued.rawToken());
		adminSessionRepository.flush();

		Optional<AdminSession> validated = adminSessionService.validateAndTouch(issued.rawToken());

		assertTrue(validated.isEmpty());
	}

	@Test
	void issue_storesOnlyHashNotRawToken() {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN)));

		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");

		assertNotEquals(issued.rawToken(), issued.session().getSessionTokenHash());
		assertFalse(issued.session().getSessionTokenHash().isBlank());
	}

}
