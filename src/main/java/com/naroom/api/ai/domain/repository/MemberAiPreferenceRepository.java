package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.MemberAiPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberAiPreferenceRepository extends JpaRepository<MemberAiPreference, UUID> {

	Optional<MemberAiPreference> findByMember_Id(UUID memberId);

}
