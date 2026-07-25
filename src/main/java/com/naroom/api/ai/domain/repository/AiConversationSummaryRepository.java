package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiConversationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationSummaryRepository extends JpaRepository<AiConversationSummary, UUID> {

	List<AiConversationSummary> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId);

}
