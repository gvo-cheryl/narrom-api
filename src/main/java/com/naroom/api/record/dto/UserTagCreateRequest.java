package com.naroom.api.record.dto;

import com.naroom.api.record.domain.entity.TagCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserTagCreateRequest(
		@NotNull TagCategory category,
		@NotBlank @Size(max = 80) String name) {
}
