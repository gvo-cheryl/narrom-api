package com.naroom.api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiFeedbackReportCreateRequest(
		@NotBlank @Size(max = 50) String reasonCode,
		String comment) {
}
