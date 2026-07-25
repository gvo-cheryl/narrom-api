package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiReflectionRepository extends JpaRepository<AiReflection, UUID> {

	List<AiReflection> findByEntry_IdOrderByVersionNoDesc(UUID entryId);

	Optional<AiReflection> findByEntry_IdAndVersionNo(UUID entryId, int versionNo);

}
