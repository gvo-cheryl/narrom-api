package com.naroom.api.checkin.dto;

import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.record.dto.TagResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckInResponse(
		UUID id,
		UUID entryId,
		LocalDate checkInDate,
		Short emotionIntensity,
		Short energyLevel,
		String memorableEvent,
		String gratitudeNote,
		String currentNeed,
		String freeNote,
		List<TagResponse> emotions,
		Instant createdAt,
		Instant updatedAt,
		Long version) {

	public static CheckInResponse of(CheckIn checkIn, List<TagResponse> emotions) {
		return new CheckInResponse(
				checkIn.getId(),
				checkIn.getEntry().getId(),
				checkIn.getCheckInDate(),
				checkIn.getEmotionIntensity(),
				checkIn.getEnergyLevel(),
				checkIn.getMemorableEvent(),
				checkIn.getGratitudeNote(),
				checkIn.getCurrentNeed(),
				checkIn.getFreeNote(),
				emotions,
				checkIn.getCreatedAt(),
				checkIn.getUpdatedAt(),
				checkIn.getVersion());
	}

}
