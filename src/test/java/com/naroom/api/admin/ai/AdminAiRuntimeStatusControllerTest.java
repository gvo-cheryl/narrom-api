package com.naroom.api.admin.ai;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.admin.auth.AdminSessionService;
import com.naroom.api.admin.auth.IssuedAdminSession;
import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiGenerationRun;
import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.ai.domain.repository.AiGenerationRunRepository;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
class AdminAiRuntimeStatusControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private AdminSessionService adminSessionService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private AiPromptVersionRepository aiPromptVersionRepository;

	@Autowired
	private AiJobRepository aiJobRepository;

	@Autowired
	private AiGenerationRunRepository aiGenerationRunRepository;

	@Test
	void list_reflectsActivePromptVersionsAndRecentJobAggregates() throws Exception {
		Cookie cookie = sessionCookie(Set.of(AdminRole.AI_OPERATOR));

		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, "제목", "오늘의 기록", LocalDate.now(), null, null, null));

		String commonLabel = "common-v" + System.nanoTime();
		String featureLabel = "entry-reflection-v" + System.nanoTime();
		AiPromptVersion commonPrompt = aiPromptVersionRepository.save(AiPromptVersion.forCommon(commonLabel));
		AiPromptVersion featurePrompt = aiPromptVersionRepository.save(
				AiPromptVersion.forFeature(AiFeatureType.ENTRY_REFLECTION, featureLabel, "schema-v1"));

		AiJob job = aiJobRepository.save(
				AiJob.forEntry(member, AiFeatureType.ENTRY_REFLECTION, entry, "idem-" + System.nanoTime()));
		job.markCompleted(Instant.now());
		aiJobRepository.save(job);

		AiGenerationRun run = aiGenerationRunRepository.save(
				AiGenerationRun.start(job, "gpt-5.6-luna", commonPrompt, featurePrompt, "schema-v1"));
		run.complete(1200, 300, AiSafetyGrade.NORMAL, AiSafetyGrade.NORMAL, 850, Instant.now());
		aiGenerationRunRepository.save(run);

		mockMvc.perform(get("/api/v1/admin/ai/runtime-status").cookie(cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(5))
				.andExpect(jsonPath("$.data[?(@.featureType=='ENTRY_REFLECTION')].commonPromptVersionLabel")
						.value(commonLabel))
				.andExpect(jsonPath("$.data[?(@.featureType=='ENTRY_REFLECTION')].featurePromptVersionLabel")
						.value(featureLabel))
				.andExpect(jsonPath("$.data[?(@.featureType=='ENTRY_REFLECTION')].totalJobCount")
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThanOrEqualTo(1))))
				.andExpect(jsonPath("$.data[?(@.featureType=='ENTRY_REFLECTION')].successRate")
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.notNullValue())))
				.andExpect(jsonPath("$.data[?(@.featureType=='ENTRY_REFLECTION')].avgLatencyMs")
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.notNullValue())));
	}

	private Cookie sessionCookie(Set<AdminRole> roles) {
		AdminUser adminUser = adminUserRepository.save(
				AdminUser.bootstrap("google-sub-" + System.nanoTime(), "admin@naroom.io", "관리자", roles));
		IssuedAdminSession issued = adminSessionService.issue(adminUser, "ip-hash", "ua-summary");
		return new Cookie("naroom_admin_session", issued.rawToken());
	}

}
