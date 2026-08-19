package com.naroom.api.admin.bootstrap;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

	@Mock
	private AdminUserRepository adminUserRepository;

	@Mock
	private ConfigurableApplicationContext applicationContext;

	@Test
	void execute_newSub_createsAdminUserWithRole() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(adminUserRepository, applicationContext);
		when(adminUserRepository.findByGoogleSub("verified-sub")).thenReturn(Optional.empty());

		int exitCode = runner.execute(new DefaultApplicationArguments(
				"--admin-bootstrap",
				"--admin-bootstrap-google-sub=verified-sub",
				"--admin-bootstrap-email=admin@naroom.io",
				"--admin-bootstrap-role=SUPER_ADMIN"));

		assertEquals(0, exitCode);
		verify(adminUserRepository, times(1)).save(any(AdminUser.class));
	}

	@Test
	void execute_alreadyRegisteredSub_isIdempotentAndDoesNotSaveAgain() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(adminUserRepository, applicationContext);
		AdminUser existing = AdminUser.bootstrap("verified-sub", "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN));
		when(adminUserRepository.findByGoogleSub("verified-sub")).thenReturn(Optional.of(existing));

		int exitCode = runner.execute(new DefaultApplicationArguments(
				"--admin-bootstrap",
				"--admin-bootstrap-google-sub=verified-sub",
				"--admin-bootstrap-email=admin@naroom.io",
				"--admin-bootstrap-role=SUPER_ADMIN"));

		assertEquals(0, exitCode);
		verify(adminUserRepository, never()).save(any());
	}

	@Test
	void execute_unknownRole_returnsNonZeroAndDoesNotSave() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(adminUserRepository, applicationContext);

		int exitCode = runner.execute(new DefaultApplicationArguments(
				"--admin-bootstrap",
				"--admin-bootstrap-google-sub=verified-sub",
				"--admin-bootstrap-email=admin@naroom.io",
				"--admin-bootstrap-role=NOT_A_ROLE"));

		assertEquals(1, exitCode);
		verify(adminUserRepository, never()).save(any());
	}

	@Test
	void execute_missingRequiredArgument_throwsIllegalArgument() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(adminUserRepository, applicationContext);

		org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> runner.execute(new DefaultApplicationArguments("--admin-bootstrap", "--admin-bootstrap-google-sub=verified-sub")));
	}

	@Test
	void run_withoutBootstrapOption_doesNothing() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(adminUserRepository, applicationContext);

		runner.run(new DefaultApplicationArguments("--server.port=8080"));

		verify(adminUserRepository, never()).findByGoogleSub(any());
	}

}
