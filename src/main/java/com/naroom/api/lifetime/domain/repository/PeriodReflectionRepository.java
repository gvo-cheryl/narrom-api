package com.naroom.api.lifetime.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodReflectionRepository extends JpaRepository<PeriodReflection, UUID> {

	List<PeriodReflection> findByMember_IdAndFeatureTypeAndPeriodStartOrderByVersionNoDesc(
			UUID memberId, AiFeatureType featureType, LocalDate periodStart);

	Optional<PeriodReflection> findByEntry_Id(UUID entryId);

	Optional<PeriodReflection> findByIdAndMember_Id(UUID id, UUID memberId);

}
