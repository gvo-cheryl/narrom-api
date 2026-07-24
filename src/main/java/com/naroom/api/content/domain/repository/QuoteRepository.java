package com.naroom.api.content.domain.repository;

import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

	List<Quote> findByStatus(QuoteStatus status);

	@Query("SELECT q FROM Quote q JOIN q.topics t WHERE t.id = :topicId AND q.status = :status")
	List<Quote> findByTopicIdAndStatus(@Param("topicId") UUID topicId, @Param("status") QuoteStatus status);

}
