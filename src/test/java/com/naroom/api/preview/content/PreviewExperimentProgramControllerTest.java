package com.naroom.api.preview.content;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.admin.preview.IssuedPreviewSession;
import com.naroom.api.admin.preview.PreviewSessionService;
import com.naroom.api.experiment.ExperimentProgramService;
import com.naroom.api.preview.auth.PreviewTokenAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// V16 시드(13개 코스)가 이미 적용돼 있다는 전제로, 시드 데이터의 PUBLISHED 코스를 그대로 검증한다
// (ExperimentProgramServiceTest와 동일한 방식) - preview 조회는 상태와 무관해 굳이 DRAFT 픽스처를
// 새로 만들 필요가 없다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class PreviewExperimentProgramControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PreviewSessionService previewSessionService;

	@Autowired
	private ExperimentProgramService experimentProgramService;

	@Test
	void getPreviewProgramDetail_withSelectedProgram_returnsThatProgram() throws Exception {
		UUID programId = experimentProgramService.list((short) 3, "EMOTION", null, null).stream()
				.filter(program -> program.code().equals("NOW_MIND_3"))
				.findFirst()
				.orElseThrow()
				.programId();
		IssuedPreviewSession issued = issueSession(Map.of("experimentProgram", programId));

		mockMvc.perform(get("/api/v1/preview/content/experiment-programs")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, issued.rawToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(programId.toString()))
				.andExpect(jsonPath("$.data.code").value("NOW_MIND_3"))
				.andExpect(jsonPath("$.data.missions.length()").value(3));
	}

	@Test
	void getPreviewProgramDetail_withoutSelectedProgram_returnsPreviewContentNotSelected() throws Exception {
		IssuedPreviewSession issued = issueSession(Map.of());

		mockMvc.perform(get("/api/v1/preview/content/experiment-programs")
						.header(PreviewTokenAuthenticationFilter.TOKEN_HEADER, issued.rawToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PREVIEW_CONTENT_NOT_SELECTED"));
	}

	@Test
	void getPreviewProgramDetail_withoutToken_returnsAuthenticationFailed() throws Exception {
		mockMvc.perform(get("/api/v1/preview/content/experiment-programs"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("PREVIEW_AUTHENTICATION_FAILED"));
	}

	private IssuedPreviewSession issueSession(Map<String, UUID> selectedContentVersions) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", Set.of(AdminRole.CONTENT_EDITOR)));
		return previewSessionService.issue(adminUser, selectedContentVersions, null);
	}

}
