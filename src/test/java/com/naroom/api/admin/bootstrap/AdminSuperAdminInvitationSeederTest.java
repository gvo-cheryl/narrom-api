package com.naroom.api.admin.bootstrap;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import com.naroom.api.admin.domain.repository.AdminInvitationRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSuperAdminInvitationSeederTest {

	@Mock
	private AdminUserRepository adminUserRepository;

	@Mock
	private AdminInvitationRepository adminInvitationRepository;

	@Test
	void run_blankEmail_doesNothing() {
		AdminSuperAdminInvitationSeeder seeder =
				new AdminSuperAdminInvitationSeeder(adminUserRepository, adminInvitationRepository, "");

		seeder.run(new DefaultApplicationArguments());

		verify(adminInvitationRepository, never()).save(any());
	}

	@Test
	void run_emailAlreadyHasAdminUser_isIdempotentAndDoesNotCreateInvitation() {
		when(adminUserRepository.existsByEmailIgnoreCase("owner@naroom.io")).thenReturn(true);
		AdminSuperAdminInvitationSeeder seeder =
				new AdminSuperAdminInvitationSeeder(adminUserRepository, adminInvitationRepository, "owner@naroom.io");

		seeder.run(new DefaultApplicationArguments());

		verify(adminInvitationRepository, never()).save(any());
	}

	@Test
	void run_emailAlreadyHasInvitation_isIdempotentAndDoesNotCreateAnother() {
		when(adminUserRepository.existsByEmailIgnoreCase("owner@naroom.io")).thenReturn(false);
		when(adminInvitationRepository.existsByEmailIgnoreCase("owner@naroom.io")).thenReturn(true);
		AdminSuperAdminInvitationSeeder seeder =
				new AdminSuperAdminInvitationSeeder(adminUserRepository, adminInvitationRepository, "owner@naroom.io");

		seeder.run(new DefaultApplicationArguments());

		verify(adminInvitationRepository, never()).save(any());
	}

	@Test
	void run_newEmail_createsPendingSuperAdminInvitation() {
		when(adminUserRepository.existsByEmailIgnoreCase("owner@naroom.io")).thenReturn(false);
		when(adminInvitationRepository.existsByEmailIgnoreCase("owner@naroom.io")).thenReturn(false);
		AdminSuperAdminInvitationSeeder seeder =
				new AdminSuperAdminInvitationSeeder(adminUserRepository, adminInvitationRepository, "owner@naroom.io");

		seeder.run(new DefaultApplicationArguments());

		verify(adminInvitationRepository, times(1)).save(any(AdminInvitation.class));
	}

}
