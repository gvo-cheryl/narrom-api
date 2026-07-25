package com.naroom.api.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@CheckInTextLength
public record CheckInUpsertRequest(
		@NotNull LocalDate checkInDate,
		@Min(1) @Max(5) Short emotionIntensity,
		@Min(1) @Max(5) Short energyLevel,
		@Size(max = 500) String memorableEvent,
		@Size(max = 300) String gratitudeNote,
		@Size(max = 200) String currentNeed,
		@Size(max = 500) String freeNote,
		List<UUID> emotionTagIds) {
}
