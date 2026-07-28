package com.naroom.api.lifetime.domain.repository;

import com.naroom.api.lifetime.domain.entity.PersonalSummary;
import com.naroom.api.lifetime.domain.entity.SummaryScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PersonalSummaryRepository extends JpaRepository<PersonalSummary, UUID> {

	List<PersonalSummary> findByMember_IdAndScopeAndArchivedAtIsNullOrderByCreatedAtDesc(UUID memberId, SummaryScope scope);

	List<PersonalSummary> findByMember_IdAndScopeOrderByCreatedAtDesc(UUID memberId, SummaryScope scope);

}
