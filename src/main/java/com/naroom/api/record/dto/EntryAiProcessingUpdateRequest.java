package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotNull;

public record EntryAiProcessingUpdateRequest(@NotNull Boolean allowed) {
}
