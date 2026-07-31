package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.UserProgramMission;
import com.naroom.api.experiment.domain.entity.UserProgramMissionSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProgramMissionRepository extends JpaRepository<UserProgramMission, UUID> {

	List<UserProgramMission> findByUserExperimentProgram_IdOrderByDayNumberAsc(UUID userExperimentProgramId);

	Optional<UserProgramMission> findByUserExperimentProgram_IdAndSlotStatus(
			UUID userExperimentProgramId, UserProgramMissionSlotStatus slotStatus);

	Optional<UserProgramMission> findByUserExperimentProgram_IdAndDayNumber(UUID userExperimentProgramId, short dayNumber);

	Optional<UserProgramMission> findByIdAndUserExperimentProgram_Id(UUID id, UUID userExperimentProgramId);

}
