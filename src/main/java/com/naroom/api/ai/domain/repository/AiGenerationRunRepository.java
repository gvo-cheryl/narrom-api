package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiGenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationRunRepository extends JpaRepository<AiGenerationRun, UUID> {

	List<AiGenerationRun> findByAiJob_Id(UUID aiJobId);

	Optional<AiGenerationRun> findByIdAndAiJob_Member_Id(UUID id, UUID memberId);

}
