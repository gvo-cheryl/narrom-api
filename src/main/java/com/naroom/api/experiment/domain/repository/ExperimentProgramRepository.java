package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentProgramRepository extends JpaRepository<ExperimentProgram, UUID> {

	List<ExperimentProgram> findByStatusOrderByDisplayOrderAsc(ExperimentProgramStatus status);

	List<ExperimentProgram> findByStatusAndDurationDaysOrderByDisplayOrderAsc(
			ExperimentProgramStatus status, short durationDays);

	List<ExperimentProgram> findByStatusAndPrimaryTopic_IdOrderByDisplayOrderAsc(
			ExperimentProgramStatus status, UUID primaryTopicId);

	Optional<ExperimentProgram> findByCode(String code);

}
