package com.naroom.api.ai.dto;

import com.naroom.api.ai.domain.entity.AiFeedbackHelpfulness;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiFeedbackSubmitRequest(
		@NotNull AiFeedbackHelpfulness helpfulness,
		@Size(max = 50) String reasonCode,
		String customReason) {
}
