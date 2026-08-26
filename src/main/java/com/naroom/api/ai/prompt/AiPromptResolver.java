package com.naroom.api.ai.prompt;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiPromptVersionStatus;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import com.naroom.api.ai.infra.openai.OpenAiProperties;
import org.springframework.stereotype.Component;

// 14.6절 "코드 또는 관리 파일에서 버전 관리"를 실제로 잇는 지점. 관리자가 작성해 발행(PUBLISHED)한
// content가 있으면 그것을 쓰고, 없으면 지금까지처럼 AiInstructionCatalog(코드)와 OpenAiProperties(전역
// 설정)로 되돌아간다 - 아직 아무도 발행하지 않은 기능은 기존 동작이 그대로 유지된다.
@Component
public class AiPromptResolver {

	private final AiPromptVersionRepository aiPromptVersionRepository;
	private final OpenAiProperties openAiProperties;

	public AiPromptResolver(AiPromptVersionRepository aiPromptVersionRepository, OpenAiProperties openAiProperties) {
		this.aiPromptVersionRepository = aiPromptVersionRepository;
		this.openAiProperties = openAiProperties;
	}

	public ResolvedCommonInstructions resolveCommon() {
		return aiPromptVersionRepository
				.findFirstByScopeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(AiPromptScope.COMMON, AiPromptVersionStatus.PUBLISHED)
				.map(version -> new ResolvedCommonInstructions(version.getVersionLabel(), version.getContent()))
				.orElseGet(() -> new ResolvedCommonInstructions(
						AiInstructionCatalog.COMMON_INSTRUCTIONS_VERSION, AiInstructionCatalog.COMMON_INSTRUCTIONS));
	}

	public ResolvedFeatureInstructions resolveFeature(AiFeatureType featureType) {
		return aiPromptVersionRepository
				.findFirstByFeatureTypeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(featureType, AiPromptVersionStatus.PUBLISHED)
				.map(version -> new ResolvedFeatureInstructions(
						version.getVersionLabel(),
						version.getContent(),
						version.getModelName() != null ? version.getModelName() : openAiProperties.model(),
						version.getOutputMaxLength()))
				.orElseGet(() -> new ResolvedFeatureInstructions(
						AiInstructionCatalog.featureInstructionsVersion(featureType),
						AiInstructionCatalog.featureInstructions(featureType),
						openAiProperties.model(),
						null));
	}

	public record ResolvedCommonInstructions(String versionLabel, String content) {
	}

	public record ResolvedFeatureInstructions(String versionLabel, String content, String modelName, Integer outputMaxLength) {
	}

}
