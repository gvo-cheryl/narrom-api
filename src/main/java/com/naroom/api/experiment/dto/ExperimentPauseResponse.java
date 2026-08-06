package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

public record ExperimentPauseResponse(UUID userExperimentProgramId, UserExperimentProgramStatus status) {
}
