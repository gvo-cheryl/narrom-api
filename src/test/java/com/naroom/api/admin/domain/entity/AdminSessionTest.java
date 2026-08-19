package com.naroom.api.admin.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSessionTest {

	private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

	@Test
	void isActive_freshlyIssued_returnsTrue() {
		AdminSession session = issueSession(Instant.now().plusSeconds(3600));

		assertTrue(session.isActive(Instant.now(), IDLE_TIMEOUT));
	}

	@Test
	void isActive_pastAbsoluteExpiry_returnsFalse() {
		AdminSession session = issueSession(Instant.now().minusSeconds(1));

		assertFalse(session.isActive(Instant.now(), IDLE_TIMEOUT));
	}

	@Test
	void isActive_idleBeyondTimeout_returnsFalse() {
		AdminSession session = issueSession(Instant.now().plusSeconds(3600));

		Instant farFuture = Instant.now().plus(IDLE_TIMEOUT).plusSeconds(60);

		assertFalse(session.isActive(farFuture, IDLE_TIMEOUT));
	}

	@Test
	void isActive_afterRevoke_returnsFalse() {
		AdminSession session = issueSession(Instant.now().plusSeconds(3600));
		session.revoke();

		assertFalse(session.isActive(Instant.now(), IDLE_TIMEOUT));
	}

	@Test
	void isActive_afterMarkUsed_extendsIdleWindow() {
		AdminSession session = issueSession(Instant.now().plusSeconds(3600));
		session.markUsed();

		Instant justUnderIdleTimeout = Instant.now().plus(IDLE_TIMEOUT).minusSeconds(10);

		assertTrue(session.isActive(justUnderIdleTimeout, IDLE_TIMEOUT));
	}

	private AdminSession issueSession(Instant expiresAt) {
		AdminUser adminUser = AdminUser.bootstrap("sub-1", "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN));
		return AdminSession.issue(adminUser, "hash", expiresAt, "ip-hash", "ua-summary");
	}

}
