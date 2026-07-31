package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.UserProgramMissionReplacement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserProgramMissionReplacementRepository extends JpaRepository<UserProgramMissionReplacement, UUID> {

	List<UserProgramMissionReplacement> findByUserProgramMission_IdOrderByReplacedAtDesc(UUID userProgramMissionId);

}
