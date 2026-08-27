package com.naroom.api.admin.domain.repository;

import com.naroom.api.admin.domain.entity.AdminInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, UUID> {

	Optional<AdminInvitation> findByEmailIgnoreCaseAndConsumedAtIsNullAndRevokedAtIsNull(String email);

	boolean existsByEmailIgnoreCase(String email);

	List<AdminInvitation> findAllByOrderByInvitedAtDesc();

}
