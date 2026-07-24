package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.TagInitiator;
import com.naroom.api.record.domain.entity.TagSource;
import com.naroom.api.record.domain.entity.TagState;

import java.util.UUID;

public record EntryTagResponse(UUID id, TagResponse tag, TagSource source, TagState state, TagInitiator initiatedBy) {

	public static EntryTagResponse from(EntryTag entryTag) {
		return new EntryTagResponse(
				entryTag.getId(),
				TagResponse.from(entryTag.getTag()),
				entryTag.getSource(),
				entryTag.getState(),
				entryTag.getInitiatedBy());
	}

}
