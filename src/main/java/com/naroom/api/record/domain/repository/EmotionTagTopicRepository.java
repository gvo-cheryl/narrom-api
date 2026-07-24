package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EmotionTagTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmotionTagTopicRepository extends JpaRepository<EmotionTagTopic, UUID> {

	List<EmotionTagTopic> findAllByOrderByDisplayOrderAsc();

}
