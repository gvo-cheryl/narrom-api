package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiJobRepository extends JpaRepository<AiJob, UUID> {

	Optional<AiJob> findByMember_IdAndIdempotencyKey(UUID memberId, String idempotencyKey);

}
