package com.naroom.api.ai.outcome;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;

import java.util.UUID;

public record PeriodReflectionOutcome(UUID periodReflectionId, UUID generationRunId, AiSafetyGrade outputSafetyGrade) {
}
