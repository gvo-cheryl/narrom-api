package com.naroom.api.admin.user;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminUserManagementControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void createListAndRevokeInvitation_asSuperAdmin_succeeds() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String email = "invitee-" + System.nanoTime() + "@naroom.io";

		String createResponse = mockMvc.perform(post("/api/v1/admin/admin-users/invitations").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","roles":["CONTENT_EDITOR"]}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

		mockMvc.perform(get("/api/v1/admin/admin-users/invitations").cookie(cookie))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/admin-users/invitations/" + id + "/revoke").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("REVOKED"));

		mockMvc.perform(post("/api/v1/admin/admin-users/invitations/" + id + "/revoke").cookie(cookie).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ADMIN_INVITATION_NOT_PENDING"));
	}

	@Test
	void createInvitation_duplicateEmail_returnsConflict() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));
		String email = "dup-" + System.nanoTime() + "@naroom.io";
		String body = """
				{"email":"%s","roles":["CONTENT_EDITOR"]}
				""".formatted(email);

		mockMvc.perform(post("/api/v1/admin/admin-users/invitations").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/admin/admin-users/invitations").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ADMIN_INVITATION_ALREADY_EXISTS"));
	}

	@Test
	void listAdminUsers_asSuperAdmin_returnsSelf() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/admin-users").cookie(cookie))
				.andExpect(status().isOk());
	}

	@Test
	void listAdminUsers_withContentEditorRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		mockMvc.perform(get("/api/v1/admin/admin-users").cookie(cookie))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
