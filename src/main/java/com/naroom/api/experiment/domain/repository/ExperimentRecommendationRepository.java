package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentRecommendation;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationSourceType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentRecommendationRepository extends JpaRepository<ExperimentRecommendation, UUID> {

	List<ExperimentRecommendation> findByMember_IdAndStatusOrderByCreatedAtDesc(
			UUID memberId, ExperimentRecommendationStatus status);

	List<ExperimentRecommendation> findByMember_IdAndStatusInOrderByCreatedAtDesc(
			UUID memberId, Collection<ExperimentRecommendationStatus> statuses);

	Optional<ExperimentRecommendation> findByIdAndMember_Id(UUID id, UUID memberId);

	// §14 중복 생성 방지: 같은 회원·코스·출처·이유로 최근에 이미 추천했다면 상태와 무관하게 다시 만들지
	// 않는다 - 넘긴 추천이 매번 다시 나타나거나, 만료 처리를 별도로 하지 않아도 오래된 추천 위에 계속
	// 새로 쌓이는 것을 막는다.
	boolean existsByMember_IdAndProgram_IdAndSourceTypeAndReasonCodeAndCreatedAtAfter(
			UUID memberId, UUID programId, ExperimentRecommendationSourceType sourceType, String reasonCode, Instant since);

}
