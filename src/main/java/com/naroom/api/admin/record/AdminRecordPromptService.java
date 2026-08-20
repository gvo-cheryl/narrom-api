package com.naroom.api.admin.record;

import com.naroom.api.admin.record.dto.AdminRecordPromptCreateRequest;
import com.naroom.api.admin.record.dto.AdminRecordPromptResponse;
import com.naroom.api.admin.record.dto.AdminRecordPromptUpdateRequest;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.RecordPrompt;
import com.naroom.api.record.domain.entity.RecordPromptStatus;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.RecordPromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminRecordPromptService {

	private final RecordPromptRepository recordPromptRepository;

	public AdminRecordPromptService(RecordPromptRepository recordPromptRepository) {
		this.recordPromptRepository = recordPromptRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminRecordPromptResponse> list() {
		return recordPromptRepository.findAllByOrderByCodeAscVersionNoDesc().stream()
				.map(AdminRecordPromptResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminRecordPromptResponse get(UUID id) {
		return AdminRecordPromptResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminRecordPromptResponse create(AdminRecordPromptCreateRequest request, UUID actingAdminId) {
		if (recordPromptRepository.existsByCode(request.code())) {
			throw new BusinessException(RecordErrorCode.PROMPT_CODE_ALREADY_EXISTS);
		}
		RecordPrompt prompt = RecordPrompt.create(
				request.code(), 1, request.questionText(), request.helperText(), request.entryType(),
				request.displayOrder(), request.activeFrom(), request.activeUntil(), null, actingAdminId);
		return AdminRecordPromptResponse.from(recordPromptRepository.save(prompt));
	}

	@Transactional
	public AdminRecordPromptResponse update(UUID id, AdminRecordPromptUpdateRequest request) {
		RecordPrompt prompt = findOrThrow(id);
		requireStatus(prompt, RecordPromptStatus.DRAFT);
		prompt.updateDraft(
				request.questionText(), request.helperText(), request.entryType(),
				request.displayOrder(), request.activeFrom(), request.activeUntil());
		return AdminRecordPromptResponse.from(prompt);
	}

	// §19.4 편집 API 버전 규칙: 발행본은 직접 수정하지 않고, 발행본 내용을 시작점으로 하는 새 DRAFT를 만든다.
	@Transactional
	public AdminRecordPromptResponse createRevision(UUID publishedId, UUID actingAdminId) {
		RecordPrompt published = findOrThrow(publishedId);
		requireStatus(published, RecordPromptStatus.PUBLISHED);
		RecordPrompt draft = RecordPrompt.create(
				published.getCode(), published.getVersionNo() + 1, published.getQuestionText(),
				published.getHelperText(), published.getEntryType(), published.getDisplayOrder(),
				published.getActiveFrom(), published.getActiveUntil(), published.getId(), actingAdminId);
		return AdminRecordPromptResponse.from(recordPromptRepository.save(draft));
	}

	// 같은 code로 이미 PUBLISHED된 버전이 있으면 먼저 ARCHIVED로 내린다(record_prompts는 code당 PUBLISHED 1개만 허용).
	@Transactional
	public AdminRecordPromptResponse publish(UUID draftId) {
		RecordPrompt draft = findOrThrow(draftId);
		requireStatus(draft, RecordPromptStatus.DRAFT);
		recordPromptRepository.findByCodeAndStatus(draft.getCode(), RecordPromptStatus.PUBLISHED)
				.ifPresent(RecordPrompt::archive);
		draft.publish();
		return AdminRecordPromptResponse.from(draft);
	}

	// §9.3: 발행 질문이 0개가 되지 않도록, 마지막 발행 질문의 보관을 차단한다.
	@Transactional
	public AdminRecordPromptResponse archive(UUID id) {
		RecordPrompt prompt = findOrThrow(id);
		requireStatus(prompt, RecordPromptStatus.PUBLISHED);
		if (recordPromptRepository.countByStatus(RecordPromptStatus.PUBLISHED) <= 1) {
			throw new BusinessException(RecordErrorCode.PROMPT_LAST_PUBLISHED_CANNOT_BE_ARCHIVED);
		}
		prompt.archive();
		return AdminRecordPromptResponse.from(prompt);
	}

	private void requireStatus(RecordPrompt prompt, RecordPromptStatus expected) {
		if (prompt.getStatus() != expected) {
			throw new BusinessException(
					expected == RecordPromptStatus.DRAFT
							? RecordErrorCode.PROMPT_NOT_DRAFT
							: RecordErrorCode.PROMPT_NOT_PUBLISHED);
		}
	}

	private RecordPrompt findOrThrow(UUID id) {
		return recordPromptRepository.findById(id)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.PROMPT_NOT_FOUND));
	}

}
