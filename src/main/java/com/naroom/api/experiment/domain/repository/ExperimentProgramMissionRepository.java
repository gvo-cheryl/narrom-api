package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperimentProgramMissionRepository
		extends JpaRepository<ExperimentProgramMission, ExperimentProgramMissionId> {

	List<ExperimentProgramMission> findByProgram_IdOrderById_DayNumberAsc(UUID programId);

	void deleteByProgram_Id(UUID programId);

}
