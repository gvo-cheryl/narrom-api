package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentMissionRepository extends JpaRepository<ExperimentMission, UUID>, JpaSpecificationExecutor<ExperimentMission> {

	Optional<ExperimentMission> findByCode(String code);

	List<ExperimentMission> findByActiveTrue();

}
