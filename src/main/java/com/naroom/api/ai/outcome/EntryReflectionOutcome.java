package com.naroom.api.ai.outcome;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;

import java.util.UUID;

public record EntryReflectionOutcome(UUID reflectionId, UUID generationRunId, AiSafetyGrade outputSafetyGrade) {
}
