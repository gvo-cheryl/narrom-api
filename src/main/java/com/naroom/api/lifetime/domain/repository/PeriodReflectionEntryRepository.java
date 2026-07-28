package com.naroom.api.lifetime.domain.repository;

import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PeriodReflectionEntryRepository extends JpaRepository<PeriodReflectionEntry, PeriodReflectionEntryId> {

	List<PeriodReflectionEntry> findByPeriodReflection_Id(UUID periodReflectionId);

}
