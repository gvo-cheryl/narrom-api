package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

	List<AiConversation> findByMember_IdOrderByLastMessageAtDesc(UUID memberId);

	Optional<AiConversation> findByIdAndMember_Id(UUID id, UUID memberId);

}
