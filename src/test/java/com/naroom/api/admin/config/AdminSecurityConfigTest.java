package com.naroom.api.admin.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /api/v1/admin/**이 회원 JWT 체인이 아니라 AdminSecurityConfig 체인을 타는지, 즉 실패 시
// AUTH_REQUIRED(회원)가 아니라 ADMIN_AUTHENTICATION_FAILED(관리자)로 응답하는지 확인한다.
@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminPath_withoutSessionCookie_returnsAdminAuthenticationFailed() throws Exception {
		mockMvc.perform(get("/api/v1/admin/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_FAILED"));
	}

	@Test
	void adminPath_withInvalidSessionCookie_returnsAdminSessionExpired() throws Exception {
		mockMvc.perform(get("/api/v1/admin/dashboard").cookie(new Cookie("naroom_admin_session", "bogus-token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ADMIN_SESSION_EXPIRED"));
	}

	@Test
	void adminLoginPath_isPublic_redirectsToGoogle() throws Exception {
		mockMvc.perform(get("/api/v1/admin/auth/login/google"))
				.andExpect(status().is3xxRedirection());
	}

}
