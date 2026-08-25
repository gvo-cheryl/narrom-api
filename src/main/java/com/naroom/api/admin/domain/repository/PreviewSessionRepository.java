package com.naroom.api.admin.domain.repository;

import com.naroom.api.admin.domain.entity.PreviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PreviewSessionRepository extends JpaRepository<PreviewSession, UUID> {

	Optional<PreviewSession> findByTokenHash(String tokenHash);

}
