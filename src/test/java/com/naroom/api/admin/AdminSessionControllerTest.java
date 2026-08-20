package com.naroom.api.admin;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminSessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void session_withValidCookie_returnsAdminIdentity() throws Exception {
		AdminUser adminUser = adminUserRepository.save(AdminUser.bootstrap(
				"google-sub-" + System.nanoTime(), "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN)));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");

		mockMvc.perform(get("/api/v1/admin/auth/session")
						.cookie(new Cookie("naroom_admin_session", issued.rawToken())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.adminId").value(adminUser.getId().toString()))
				.andExpect(jsonPath("$.data.email").value("admin@naroom.io"))
				.andExpect(jsonPath("$.data.displayName").value("지연"))
				.andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"));
	}

}
