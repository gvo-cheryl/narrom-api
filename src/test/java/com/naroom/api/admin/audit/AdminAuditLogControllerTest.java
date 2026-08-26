package com.naroom.api.admin.audit;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminAuditLog;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminAuditLogRepository;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminAuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminAuditLogRepository adminAuditLogRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	// admin_audit_logs는 이 세션 전체의 실제 관리자 로그인·preview 세션 발급 등으로 이미 많은 행이 쌓여
	// 있는 공유 DB다 - 테스트마다 고유한 resourceType으로 좁혀 기존 데이터와 섞이지 않게 한다.

	@Test
	void list_withSupportReadOnlyRole_returnsRecentLogsFirst() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));
		String resourceType = "AdminAuditLogControllerTest-" + System.nanoTime();
		AdminAuditLog older = adminAuditLogRepository.save(AdminAuditLog.record(
				null, "LOGIN_FAILURE", resourceType, null, null, null, null, "trace-1", "POST",
				"/api/v1/admin/auth/login/google", AdminAuditOutcome.FAILURE));
		AdminAuditLog newer = adminAuditLogRepository.save(AdminAuditLog.record(
				null, "PREVIEW_SESSION_CREATE", resourceType, "resource-1", null, null, null, "trace-2", "POST",
				"/api/v1/admin/preview/sessions", AdminAuditOutcome.SUCCESS));

		mockMvc.perform(get("/api/v1/admin/audit-logs").param("resourceType", resourceType).cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.data[0].id").value(newer.getId().toString()))
				.andExpect(jsonPath("$.data.data[1].id").value(older.getId().toString()))
				.andExpect(jsonPath("$.data.page.hasNext").value(false))
				.andExpect(jsonPath("$.data.page.nextCursor").doesNotExist());
	}

	@Test
	void list_filteredByOutcome_returnsOnlyMatching() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));
		String resourceType = "AdminAuditLogControllerTest-" + System.nanoTime();
		adminAuditLogRepository.save(AdminAuditLog.record(
				null, "LOGIN_FAILURE", resourceType, null, null, null, null, "trace-3", "POST",
				"/api/v1/admin/auth/login/google", AdminAuditOutcome.FAILURE));
		AdminAuditLog success = adminAuditLogRepository.save(AdminAuditLog.record(
				null, "PREVIEW_SESSION_CREATE", resourceType, "resource-2", null, null, null, "trace-4", "POST",
				"/api/v1/admin/preview/sessions", AdminAuditOutcome.SUCCESS));

		mockMvc.perform(get("/api/v1/admin/audit-logs")
						.param("resourceType", resourceType).param("outcome", "SUCCESS").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.data.length()").value(1))
				.andExpect(jsonPath("$.data.data[0].id").value(success.getId().toString()));
	}

	@Test
	void list_sizeSmallerThanTotal_returnsHasNextAndCursor() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));
		String resourceType = "AdminAuditLogControllerTest-" + System.nanoTime();
		adminAuditLogRepository.save(AdminAuditLog.record(
				null, "ACTION_A", resourceType, null, null, null, null, "trace-5", "POST", "/path", AdminAuditOutcome.SUCCESS));
		adminAuditLogRepository.save(AdminAuditLog.record(
				null, "ACTION_B", resourceType, null, null, null, null, "trace-6", "POST", "/path", AdminAuditOutcome.SUCCESS));

		mockMvc.perform(get("/api/v1/admin/audit-logs")
						.param("resourceType", resourceType).param("size", "1").cookie(cookie).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.data.length()").value(1))
				.andExpect(jsonPath("$.data.page.hasNext").value(true))
				.andExpect(jsonPath("$.data.page.nextCursor").isNotEmpty());
	}

	@Test
	void list_withContentEditorRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));

		mockMvc.perform(get("/api/v1/admin/audit-logs").cookie(cookie).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
	}

	@Test
	void list_withInvalidCursor_returnsValidationFailed() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));

		mockMvc.perform(get("/api/v1/admin/audit-logs").param("cursor", "not-a-valid-cursor").cookie(cookie).with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
