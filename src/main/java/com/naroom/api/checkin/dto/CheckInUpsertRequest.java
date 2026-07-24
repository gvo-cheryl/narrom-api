package com.naroom.api.checkin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckInUpsertRequest(
		@NotNull LocalDate checkInDate,
		@Min(1) @Max(5) Short emotionIntensity,
		@Min(1) @Max(5) Short energyLevel,
		String memorableEvent,
		String gratitudeNote,
		String currentNeed,
		String freeNote,
		List<UUID> emotionTagIds) {
}
