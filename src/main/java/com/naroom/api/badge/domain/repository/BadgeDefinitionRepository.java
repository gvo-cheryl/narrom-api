package com.naroom.api.badge.domain.repository;

import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.entity.BadgeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, UUID> {

	Optional<BadgeDefinition> findByCode(BadgeCode code);

	List<BadgeDefinition> findAllByOrderByDisplayOrderAsc();

}
