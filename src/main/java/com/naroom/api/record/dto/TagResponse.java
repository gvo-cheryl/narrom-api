package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;

import java.util.UUID;

public record TagResponse(UUID id, TagScope scope, TagCategory category, String name) {

	public static TagResponse from(Tag tag) {
		return new TagResponse(tag.getId(), tag.getScope(), tag.getCategory(), tag.getName());
	}

}
