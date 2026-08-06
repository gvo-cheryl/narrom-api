package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

import java.util.UUID;

public record ExperimentEndEarlyResponse(UUID userExperimentProgramId, UserExperimentProgramStatus status) {
}
