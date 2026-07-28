package com.naroom.api.ai.outcome;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.repository.AiPromptVersionRepository;
import org.springframework.stereotype.Component;

// EntryReflectionOutcomeService/PeriodReflectionOutcomeService가 공통으로 쓰는 get-or-create 로직을
// 한 곳에 모은 것(3-C, 2026-07-28) - 두 서비스가 각자 같은 조회·생성 코드를 중복해서 갖지 않게 한다.
@Component
class AiPromptVersionResolver {

	private final AiPromptVersionRepository aiPromptVersionRepository;

	AiPromptVersionResolver(AiPromptVersionRepository aiPromptVersionRepository) {
		this.aiPromptVersionRepository = aiPromptVersionRepository;
	}

	AiPromptVersion getOrCreateCommon(String versionLabel) {
		return aiPromptVersionRepository.findByScopeAndVersionLabel(AiPromptScope.COMMON, versionLabel)
				.orElseGet(() -> aiPromptVersionRepository.save(AiPromptVersion.forCommon(versionLabel)));
	}

	AiPromptVersion getOrCreateFeature(AiFeatureType featureType, String versionLabel, String outputSchemaVersion) {
		return aiPromptVersionRepository.findByFeatureTypeAndVersionLabel(featureType, versionLabel)
				.orElseGet(() -> aiPromptVersionRepository.save(AiPromptVersion.forFeature(featureType, versionLabel, outputSchemaVersion)));
	}

}
