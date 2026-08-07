package com.naroom.api.badge.domain.repository;

import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.entity.MemberBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberBadgeRepository extends JpaRepository<MemberBadge, UUID> {

	boolean existsByMember_IdAndBadgeDefinition_Code(UUID memberId, BadgeCode code);

	List<MemberBadge> findByMember_IdOrderByEarnedAtDesc(UUID memberId);

}
