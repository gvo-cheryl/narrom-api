package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EntrySelfReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EntrySelfReflectionRepository extends JpaRepository<EntrySelfReflection, UUID> {

	List<EntrySelfReflection> findByEntry_IdOrderByCreatedAtDesc(UUID entryId);

}
