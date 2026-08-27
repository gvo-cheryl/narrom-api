package com.naroom.api.admin.domain.repository;

import com.naroom.api.admin.domain.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

	Optional<AdminUser> findByGoogleSub(String googleSub);

	boolean existsByEmailIgnoreCase(String email);

	List<AdminUser> findAllByOrderByCreatedAtDesc();

}
