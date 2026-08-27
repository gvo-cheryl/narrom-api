package com.naroom.api.admin.record;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminRecordContentLimitControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void getAndUpdate_asSuperAdmin_updatesBodyMaxLength() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(get("/api/v1/admin/record/content-limits").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bodyMaxLength").isNumber());

		mockMvc.perform(put("/api/v1/admin/record/content-limits").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"bodyMaxLength\":3000}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bodyMaxLength").value(3000));

		mockMvc.perform(get("/api/v1/admin/record/content-limits").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.bodyMaxLength").value(3000));
	}

	@Test
	void update_bodyMaxLengthOverHardCeiling_returnsBadRequest() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPER_ADMIN));

		mockMvc.perform(put("/api/v1/admin/record/content-limits").cookie(cookie).with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"bodyMaxLength\":20001}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void get_withContentEditorRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		mockMvc.perform(get("/api/v1/admin/record/content-limits").cookie(cookie))
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
