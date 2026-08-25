package com.naroom.api.admin.preview;

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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminPreviewSessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void create_withContentEditorRole_returnsToken() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.CONTENT_EDITOR));
		String quoteVersionId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/admin/preview/sessions")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"selectedContentVersions":{"quote":"%s"},"scenarioKey":"first-visit"}
								""".formatted(quoteVersionId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.token").isNotEmpty())
				.andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
	}

	@Test
	void create_withoutContentPermissionRole_returnsAccessDenied() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.SUPPORT_READ_ONLY));

		mockMvc.perform(post("/api/v1/admin/preview/sessions")
						.cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"selectedContentVersions":{}}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ADMIN_ACCESS_DENIED"));
	}

	// AdminAuditLogService.record()는 REQUIRES_NEW로 별도 트랜잭션·커넥션을 연다 - 이 테스트 메서드의
	// 바깥 트랜잭션에 아직 커밋되지 않은 admin_user row는 그 별도 커넥션에서 보이지 않아 FK 위반이 난다.
	// 여기서 admin_user 생성만 먼저 커밋해 실제 운영 시나리오(로그인으로 이미 존재하는 관리자)와 맞춘다.
	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
