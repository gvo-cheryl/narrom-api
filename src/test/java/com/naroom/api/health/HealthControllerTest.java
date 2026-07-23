package com.naroom.api.health;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SecurityConfig의 SecurityFilterChain이 WebMvcTest 슬라이스에도 자동 포함되고,
// 그 안의 JwtAuthenticationFilter(Filter 타입이라 슬라이스 기본 포함 대상)가
// JwtTokenProvider/AuthSessionRepository(JPA)를 요구해서 mock으로 채워준다.
// health는 permitAll 경로라 이 필터가 실제로 뭘 하든 이 테스트와는 무관하다.
@WebMvcTest(HealthController.class)
@Import(ProblemDetailFactory.class)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void health_returns200AndStatusUp() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UP"));
	}

}
