package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntryUpdateRequest(
		@Size(max = 200) String title,
		String body,
		@NotNull Long version) {
}
