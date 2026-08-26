package com.naroom.api.admin.ai;

import com.naroom.api.admin.ai.dto.AdminAiPromptCreateRequest;
import com.naroom.api.admin.ai.dto.AdminAiPromptResponse;
import com.naroom.api.admin.ai.dto.AdminAiPromptUpdateRequest;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiPromptVersionStatus;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 14.6절: 관리자가 발행(PUBLISHED)한 content가 실제 AI 호출에 쓰인다(AiPromptResolver 참고). 발행본은
// 직접 수정하지 않고 새 DRAFT를 만들어 검토 후 발행한다 - quotes/experiment programs와 같은 버전 규칙.
@Service
public class AdminAiPromptService {

	private final AiPromptVersionRepository aiPromptVersionRepository;

	public AdminAiPromptService(AiPromptVersionRepository aiPromptVersionRepository) {
		this.aiPromptVersionRepository = aiPromptVersionRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminAiPromptResponse> list() {
		return aiPromptVersionRepository.findByContentIsNotNullOrderByScopeAscFeatureTypeAscCreatedAtDesc().stream()
				.map(AdminAiPromptResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminAiPromptResponse get(UUID id) {
		return AdminAiPromptResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminAiPromptResponse create(AdminAiPromptCreateRequest request, UUID actingAdminId) {
		requireLabelAvailable(request.scope(), request.featureType(), request.versionLabel(), null);
		AiPromptVersion version = request.scope() == AiPromptScope.COMMON
				? AiPromptVersion.draftCommon(request.versionLabel(), request.content(), actingAdminId)
				: AiPromptVersion.draftFeature(
						request.featureType(), request.versionLabel(), request.content(),
						request.modelName(), request.outputMaxLength(), null, actingAdminId);
		return AdminAiPromptResponse.from(aiPromptVersionRepository.save(version));
	}

	@Transactional
	public AdminAiPromptResponse update(UUID id, AdminAiPromptUpdateRequest request) {
		AiPromptVersion version = findOrThrow(id);
		requireStatus(version, AiPromptVersionStatus.DRAFT);
		requireLabelAvailable(version.getScope(), version.getFeatureType(), request.versionLabel(), id);
		version.updateDraft(
				request.versionLabel(), request.content(),
				version.getScope() == AiPromptScope.FEATURE ? request.modelName() : null,
				version.getScope() == AiPromptScope.FEATURE ? request.outputMaxLength() : null);
		return AdminAiPromptResponse.from(version);
	}

	@Transactional
	public AdminAiPromptResponse createRevision(UUID publishedId, String newVersionLabel, UUID actingAdminId) {
		AiPromptVersion published = findOrThrow(publishedId);
		requireStatus(published, AiPromptVersionStatus.PUBLISHED);
		requireLabelAvailable(published.getScope(), published.getFeatureType(), newVersionLabel, null);
		AiPromptVersion draft = published.getScope() == AiPromptScope.COMMON
				? AiPromptVersion.draftCommonRevision(newVersionLabel, published.getContent(), published.getId(), actingAdminId)
				: AiPromptVersion.draftFeature(
						published.getFeatureType(), newVersionLabel, published.getContent(),
						published.getModelName(), published.getOutputMaxLength(), published.getId(), actingAdminId);
		return AdminAiPromptResponse.from(aiPromptVersionRepository.save(draft));
	}

	// 같은 (scope, featureType) 슬롯에 이미 발행된 관리자 content가 있으면 먼저 ARCHIVED로 내린다
	// (quotes가 code당 PUBLISHED 1개만 허용하는 것과 동일한 규칙).
	@Transactional
	public AdminAiPromptResponse publish(UUID draftId) {
		AiPromptVersion draft = findOrThrow(draftId);
		requireStatus(draft, AiPromptVersionStatus.DRAFT);
		currentPublished(draft.getScope(), draft.getFeatureType()).ifPresent(AiPromptVersion::archive);
		draft.publish();
		return AdminAiPromptResponse.from(draft);
	}

	@Transactional
	public AdminAiPromptResponse archive(UUID id) {
		AiPromptVersion version = findOrThrow(id);
		requireStatus(version, AiPromptVersionStatus.PUBLISHED);
		version.archive();
		return AdminAiPromptResponse.from(version);
	}

	private Optional<AiPromptVersion> currentPublished(AiPromptScope scope, AiFeatureType featureType) {
		return scope == AiPromptScope.COMMON
				? aiPromptVersionRepository.findFirstByScopeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(
						AiPromptScope.COMMON, AiPromptVersionStatus.PUBLISHED)
				: aiPromptVersionRepository.findFirstByFeatureTypeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(
						featureType, AiPromptVersionStatus.PUBLISHED);
	}

	private void requireLabelAvailable(AiPromptScope scope, AiFeatureType featureType, String versionLabel, UUID excludingId) {
		Optional<AiPromptVersion> existing = scope == AiPromptScope.COMMON
				? aiPromptVersionRepository.findByScopeAndVersionLabel(AiPromptScope.COMMON, versionLabel)
				: aiPromptVersionRepository.findByFeatureTypeAndVersionLabel(featureType, versionLabel);
		if (existing.isPresent() && !existing.get().getId().equals(excludingId)) {
			throw new BusinessException(AiErrorCode.PROMPT_VERSION_LABEL_ALREADY_EXISTS);
		}
	}

	private void requireStatus(AiPromptVersion version, AiPromptVersionStatus expected) {
		if (version.getStatus() != expected) {
			throw new BusinessException(
					expected == AiPromptVersionStatus.DRAFT
							? AiErrorCode.PROMPT_VERSION_NOT_DRAFT
							: AiErrorCode.PROMPT_VERSION_NOT_PUBLISHED);
		}
	}

	private AiPromptVersion findOrThrow(UUID id) {
		return aiPromptVersionRepository.findById(id)
				.orElseThrow(() -> new BusinessException(AiErrorCode.PROMPT_VERSION_NOT_FOUND));
	}

}
