package com.naroom.api.record.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EntryTagAttachRequest(@NotNull UUID tagId) {
}
