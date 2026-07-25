package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiPromptScope;
import com.naroom.api.ai.domain.entity.AiPromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiPromptVersionRepository extends JpaRepository<AiPromptVersion, UUID> {

	List<AiPromptVersion> findByScopeAndActiveTrue(AiPromptScope scope);

	List<AiPromptVersion> findByFeatureTypeAndActiveTrue(AiFeatureType featureType);

}
