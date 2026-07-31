package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentRecommendation;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentRecommendationRepository extends JpaRepository<ExperimentRecommendation, UUID> {

	List<ExperimentRecommendation> findByMember_IdAndStatusOrderByCreatedAtDesc(
			UUID memberId, ExperimentRecommendationStatus status);

	Optional<ExperimentRecommendation> findByIdAndMember_Id(UUID id, UUID memberId);

}
