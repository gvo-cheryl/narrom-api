package com.naroom.api.content.domain.repository;

import com.naroom.api.content.domain.entity.QuoteTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuoteTopicRepository extends JpaRepository<QuoteTopic, UUID> {

	List<QuoteTopic> findByActiveTrue();

}
