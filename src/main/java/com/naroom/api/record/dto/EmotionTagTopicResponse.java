package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.EmotionTagTopic;

import java.util.List;
import java.util.UUID;

public record EmotionTagTopicResponse(UUID id, String code, String name, List<TagResponse> tags) {

	public static EmotionTagTopicResponse of(EmotionTagTopic topic, List<TagResponse> tags) {
		return new EmotionTagTopicResponse(topic.getId(), topic.getCode(), topic.getName(), tags);
	}

}
