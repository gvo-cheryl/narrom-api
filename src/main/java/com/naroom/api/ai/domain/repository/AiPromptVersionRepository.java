package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import com.naroom.api.ai.domain.entity.AiPromptVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiPromptVersionRepository extends JpaRepository<AiPromptVersion, UUID> {

	List<AiPromptVersion> findByScopeAndStatus(AiPromptScope scope, AiPromptVersionStatus status);

	List<AiPromptVersion> findByFeatureTypeAndStatus(AiFeatureType featureType, AiPromptVersionStatus status);

	Optional<AiPromptVersion> findByScopeAndVersionLabel(AiPromptScope scope, String versionLabel);

	Optional<AiPromptVersion> findByFeatureTypeAndVersionLabel(AiFeatureType featureType, String versionLabel);

	// content가 있는 row만 관리자가 작성한 실제 지침이다(코드 북마킹용 row와 구분).
	Optional<AiPromptVersion> findFirstByScopeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(
			AiPromptScope scope, AiPromptVersionStatus status);

	Optional<AiPromptVersion> findFirstByFeatureTypeAndContentIsNotNullAndStatusOrderByCreatedAtDesc(
			AiFeatureType featureType, AiPromptVersionStatus status);

	List<AiPromptVersion> findByContentIsNotNullOrderByScopeAscFeatureTypeAscCreatedAtDesc();

}
