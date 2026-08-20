package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.RecordPrompt;
import com.naroom.api.record.domain.entity.RecordPromptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordPromptRepository extends JpaRepository<RecordPrompt, UUID> {

	boolean existsByCode(String code);

	Optional<RecordPrompt> findByCodeAndStatus(String code, RecordPromptStatus status);

	long countByStatus(RecordPromptStatus status);

	List<RecordPrompt> findAllByOrderByCodeAscVersionNoDesc();

}
