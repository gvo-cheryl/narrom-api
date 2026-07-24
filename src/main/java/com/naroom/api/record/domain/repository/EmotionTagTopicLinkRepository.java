package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EmotionTagTopicLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmotionTagTopicLinkRepository extends JpaRepository<EmotionTagTopicLink, UUID> {

	List<EmotionTagTopicLink> findAllByOrderByTopic_DisplayOrderAscDisplayOrderAsc();

}
