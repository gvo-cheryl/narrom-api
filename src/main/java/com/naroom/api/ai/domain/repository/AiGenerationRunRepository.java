package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiGenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiGenerationRunRepository extends JpaRepository<AiGenerationRun, UUID> {

	List<AiGenerationRun> findByAiJob_Id(UUID aiJobId);

}
