package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotBlank;

public record EntrySelfReflectionRequest(@NotBlank String content) {
}
