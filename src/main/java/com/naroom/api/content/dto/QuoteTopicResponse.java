package com.naroom.api.content.dto;

import com.naroom.api.content.domain.entity.QuoteTopic;

import java.util.UUID;

public record QuoteTopicResponse(UUID id, String code, String name) {

	public static QuoteTopicResponse from(QuoteTopic topic) {
		return new QuoteTopicResponse(topic.getId(), topic.getCode(), topic.getName());
	}

}
