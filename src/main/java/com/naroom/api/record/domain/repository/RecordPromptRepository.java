package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.RecordPrompt;
import com.naroom.api.record.domain.entity.RecordPromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordPromptRepository extends JpaRepository<RecordPrompt, UUID>, JpaSpecificationExecutor<RecordPrompt> {

	boolean existsByCode(String code);

	Optional<RecordPrompt> findByCodeAndStatus(String code, RecordPromptStatus status);

	long countByStatus(RecordPromptStatus status);

}
