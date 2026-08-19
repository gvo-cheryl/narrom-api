package com.naroom.api.admin.domain.repository;

import com.naroom.api.admin.domain.entity.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminSessionRepository extends JpaRepository<AdminSession, UUID> {

	Optional<AdminSession> findBySessionTokenHash(String sessionTokenHash);

}
