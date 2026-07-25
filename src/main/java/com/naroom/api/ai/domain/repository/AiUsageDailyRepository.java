package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageDailyRepository extends JpaRepository<AiUsageDaily, UUID> {

	Optional<AiUsageDaily> findByMember_IdAndUsageDateAndFeatureTypeAndModelName(
			UUID memberId, LocalDate usageDate, AiFeatureType featureType, String modelName);

}
