package com.naroom.api.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiFeedbackLongTermRequest(@NotNull Boolean applyLongTerm) {
}
