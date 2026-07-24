package com.naroom.api.record;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.global.response.ApiResponse;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.dto.EmotionTagTopicResponse;
import com.naroom.api.record.dto.EntryCreateRequest;
import com.naroom.api.record.dto.EntryResponse;
import com.naroom.api.record.dto.EntrySelfReflectionRequest;
import com.naroom.api.record.dto.EntrySelfReflectionResponse;
import com.naroom.api.record.dto.EntryTagAttachRequest;
import com.naroom.api.record.dto.EntryTagResponse;
import com.naroom.api.record.dto.EntryUpdateRequest;
import com.naroom.api.record.dto.TagResponse;
import com.naroom.api.record.dto.UserTagCreateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/record")
public class RecordController {

	private final TagService tagService;
	private final EntryService entryService;
	private final EntryTagService entryTagService;
	private final EntrySelfReflectionService entrySelfReflectionService;

	public RecordController(
			TagService tagService,
			EntryService entryService,
			EntryTagService entryTagService,
			EntrySelfReflectionService entrySelfReflectionService) {
		this.tagService = tagService;
		this.entryService = entryService;
		this.entryTagService = entryTagService;
		this.entrySelfReflectionService = entrySelfReflectionService;
	}

	@GetMapping("/tags/system")
	public ApiResponse<List<TagResponse>> getSystemTags() {
		return ApiResponse.of(tagService.listSystemTags());
	}

	@GetMapping("/tags/mine")
	public ApiResponse<List<TagResponse>> getMyTags() {
		return ApiResponse.of(tagService.listUserTags(currentMemberId()));
	}

	@PostMapping("/tags")
	public ApiResponse<TagResponse> createMyTag(@Valid @RequestBody UserTagCreateRequest request) {
		return ApiResponse.of(tagService.createUserTag(currentMemberId(), request));
	}

	@GetMapping("/tags/emotion-topics")
	public ApiResponse<List<EmotionTagTopicResponse>> getEmotionTagTopics() {
		return ApiResponse.of(tagService.listEmotionTagTopics());
	}

	@PostMapping("/entries")
	public ApiResponse<EntryResponse> createEntry(@Valid @RequestBody EntryCreateRequest request) {
		return ApiResponse.of(entryService.createEntry(currentMemberId(), request));
	}

	@GetMapping("/entries")
	public ApiResponse<List<EntryResponse>> getEntries(
			@RequestParam(required = false) EntryType entryType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate) {
		return ApiResponse.of(entryService.listEntries(currentMemberId(), entryType, recordDate));
	}

	@GetMapping("/entries/{entryId}")
	public ApiResponse<EntryResponse> getEntry(@PathVariable UUID entryId) {
		return ApiResponse.of(entryService.getEntry(currentMemberId(), entryId));
	}

	@PatchMapping("/entries/{entryId}")
	public ApiResponse<EntryResponse> updateEntry(@PathVariable UUID entryId, @Valid @RequestBody EntryUpdateRequest request) {
		return ApiResponse.of(entryService.updateEntry(currentMemberId(), entryId, request));
	}

	@PostMapping("/entries/{entryId}/publish")
	public ApiResponse<EntryResponse> publishEntry(@PathVariable UUID entryId) {
		return ApiResponse.of(entryService.publishEntry(currentMemberId(), entryId));
	}

	@DeleteMapping("/entries/{entryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEntry(@PathVariable UUID entryId) {
		entryService.deleteEntry(currentMemberId(), entryId);
	}

	@GetMapping("/entries/{entryId}/tags")
	public ApiResponse<List<EntryTagResponse>> getEntryTags(@PathVariable UUID entryId) {
		return ApiResponse.of(entryTagService.listEntryTags(currentMemberId(), entryId));
	}

	@PostMapping("/entries/{entryId}/tags")
	public ApiResponse<EntryTagResponse> attachEntryTag(
			@PathVariable UUID entryId, @Valid @RequestBody EntryTagAttachRequest request) {
		return ApiResponse.of(entryTagService.attachUserTag(currentMemberId(), entryId, request.tagId()));
	}

	@PostMapping("/entries/{entryId}/tags/{entryTagId}/confirm")
	public ApiResponse<EntryTagResponse> confirmEntryTag(@PathVariable UUID entryId, @PathVariable UUID entryTagId) {
		return ApiResponse.of(entryTagService.confirmTag(currentMemberId(), entryId, entryTagId));
	}

	@PostMapping("/entries/{entryId}/tags/{entryTagId}/reject")
	public ApiResponse<EntryTagResponse> rejectEntryTag(@PathVariable UUID entryId, @PathVariable UUID entryTagId) {
		return ApiResponse.of(entryTagService.rejectTag(currentMemberId(), entryId, entryTagId));
	}

	@GetMapping("/entries/{entryId}/reflections")
	public ApiResponse<List<EntrySelfReflectionResponse>> getReflections(@PathVariable UUID entryId) {
		return ApiResponse.of(entrySelfReflectionService.listReflections(currentMemberId(), entryId));
	}

	@PostMapping("/entries/{entryId}/reflections")
	public ApiResponse<EntrySelfReflectionResponse> createReflection(
			@PathVariable UUID entryId, @Valid @RequestBody EntrySelfReflectionRequest request) {
		return ApiResponse.of(entrySelfReflectionService.createReflection(currentMemberId(), entryId, request.content()));
	}

	@PatchMapping("/entries/{entryId}/reflections/{reflectionId}")
	public ApiResponse<EntrySelfReflectionResponse> updateReflection(
			@PathVariable UUID entryId, @PathVariable UUID reflectionId, @Valid @RequestBody EntrySelfReflectionRequest request) {
		return ApiResponse.of(
				entrySelfReflectionService.updateReflection(currentMemberId(), entryId, reflectionId, request.content()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AccountController/ContentController와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
