package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, UUID> {

	Optional<AiFeedback> findByMember_IdAndGenerationRun_Id(UUID memberId, UUID generationRunId);

}
