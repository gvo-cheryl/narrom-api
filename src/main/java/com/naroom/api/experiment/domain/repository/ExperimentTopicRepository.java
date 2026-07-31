package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentTopicRepository extends JpaRepository<ExperimentTopic, UUID> {

	List<ExperimentTopic> findByActiveTrueOrderByDisplayOrderAsc();

	Optional<ExperimentTopic> findByCode(String code);

}
