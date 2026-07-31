package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.MissionReplacementReasonCode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ExperimentMissionReplaceRequest(
		@NotNull UUID replacementMissionId,
		MissionReplacementReasonCode reasonCode,
		String reasonNote) {
}
