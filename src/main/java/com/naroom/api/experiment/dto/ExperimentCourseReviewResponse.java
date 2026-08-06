package com.naroom.api.experiment.dto;

import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;

public record ExperimentCourseReviewResponse(
		UserExperimentProgramStatus status,
		boolean lifeTimeEntryCreated,
		ExperimentAiJobSummary aiJob) {
}
