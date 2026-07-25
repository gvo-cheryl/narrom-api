package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntrySelfReflectionRequest(@NotBlank @Size(max = 1000) String content) {
}
