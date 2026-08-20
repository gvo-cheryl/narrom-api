package com.naroom.api.admin;

import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 실제 임베디드 서버로 요청을 보내는 통합 테스트다 - MockMvc(@Transactional)는 테스트 트랜잭션이 요청 전체를
// 감싸버려서, open-in-view: false 환경에서 필터가 LAZY 연관을 트랜잭션 밖에서 읽다 던지는
// LazyInitializationException을 실제로는 재현하지 못했다(AdminSessionAuthenticationFilter 참고).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminSessionRealHttpTest {

	@LocalServerPort
	private int port;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Test
	void session_realHttpRoundTrip_returnsOkWithoutLazyInitializationException() throws IOException, InterruptedException {
		AdminUser adminUser = adminUserRepository.save(AdminUser.bootstrap(
				"google-sub-" + System.nanoTime(), "admin@naroom.io", "지연", Set.of(AdminRole.SUPER_ADMIN)));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/v1/admin/auth/session"))
				.header("Cookie", "naroom_admin_session=" + issued.rawToken())
				.GET()
				.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode(), response.body());
	}

}
