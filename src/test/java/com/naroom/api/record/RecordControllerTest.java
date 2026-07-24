package com.naroom.api.record;

import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.config.SecurityConfig;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.global.error.response.ProblemDetailFactory;
import com.naroom.api.global.security.ApiAccessDeniedHandler;
import com.naroom.api.global.security.ApiAuthenticationEntryPoint;
import com.naroom.api.global.security.SecurityProblemWriter;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.dto.EntryResponse;
import com.naroom.api.record.dto.TagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecordController.class)
@Import({
		ProblemDetailFactory.class,
		SecurityConfig.class,
		ApiAuthenticationEntryPoint.class,
		ApiAccessDeniedHandler.class,
		SecurityProblemWriter.class
})
class RecordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TagService tagService;

	@MockitoBean
	private EntryService entryService;

	@MockitoBean
	private EntryTagService entryTagService;

	@MockitoBean
	private EntrySelfReflectionService entrySelfReflectionService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthSessionRepository authSessionRepository;

	@Test
	void getSystemTags_withoutAuthentication_returnsAuthRequired() throws Exception {
		mockMvc.perform(get("/api/v1/record/tags/system"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void getSystemTags_authenticated_returnsTags() throws Exception {
		when(tagService.listSystemTags())
				.thenReturn(List.of(new TagResponse(UUID.randomUUID(), TagScope.SYSTEM, TagCategory.EMOTION, "편안함")));

		mockMvc.perform(get("/api/v1/record/tags/system").with(authentication(memberAuthentication())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("편안함"));
	}

	@Test
	void createEntry_notUserCreatableType_returnsProblemDetail() throws Exception {
		when(entryService.createEntry(any(), any()))
				.thenThrow(new BusinessException(RecordErrorCode.ENTRY_TYPE_NOT_USER_CREATABLE));

		mockMvc.perform(post("/api/v1/record/entries")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "entryType": "CHECK_IN", "body": "본문", "recordDate": "2026-07-25" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("RECORD_ENTRY_TYPE_NOT_USER_CREATABLE"));
	}

	@Test
	void createEntry_validRequest_returnsCreatedEntry() throws Exception {
		UUID entryId = UUID.randomUUID();
		when(entryService.createEntry(any(), any())).thenReturn(sampleEntry(entryId));

		mockMvc.perform(post("/api/v1/record/entries")
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "entryType": "FREE", "body": "본문", "recordDate": "2026-07-25" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(entryId.toString()));
	}

	@Test
	void getEntry_notFound_returnsProblemDetail() throws Exception {
		when(entryService.getEntry(any(), any())).thenThrow(new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));

		mockMvc.perform(get("/api/v1/record/entries/" + UUID.randomUUID()).with(authentication(memberAuthentication())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECORD_ENTRY_NOT_FOUND"));
	}

	@Test
	void updateEntry_versionConflict_returnsProblemDetail() throws Exception {
		when(entryService.updateEntry(any(), any(), any()))
				.thenThrow(new BusinessException(RecordErrorCode.ENTRY_VERSION_CONFLICT));

		mockMvc.perform(patch("/api/v1/record/entries/" + UUID.randomUUID())
						.with(authentication(memberAuthentication()))
						.contentType("application/json")
						.content("""
								{ "title": "제목", "body": "본문", "version": 0 }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECORD_ENTRY_VERSION_CONFLICT"));
	}

	@Test
	void deleteEntry_authenticated_returnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/record/entries/" + UUID.randomUUID()).with(authentication(memberAuthentication())))
				.andExpect(status().isNoContent());
	}

	private EntryResponse sampleEntry(UUID entryId) {
		return new EntryResponse(
				entryId, EntryType.FREE, EntryStatus.DRAFT, null, "본문", LocalDate.now(),
				null, null, true, null, null, null, 0L);
	}

	private MemberAuthentication memberAuthentication() {
		return new MemberAuthentication(UUID.randomUUID(), UUID.randomUUID());
	}

}
