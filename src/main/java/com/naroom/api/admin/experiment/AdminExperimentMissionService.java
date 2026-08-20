package com.naroom.api.admin.experiment;

import com.naroom.api.admin.experiment.dto.AdminExperimentMissionCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentMissionResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentMissionUpdateRequest;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminExperimentMissionService {

	private final ExperimentMissionRepository experimentMissionRepository;
	private final ExperimentTopicRepository experimentTopicRepository;

	public AdminExperimentMissionService(
			ExperimentMissionRepository experimentMissionRepository, ExperimentTopicRepository experimentTopicRepository) {
		this.experimentMissionRepository = experimentMissionRepository;
		this.experimentTopicRepository = experimentTopicRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminExperimentMissionResponse> list() {
		return experimentMissionRepository.findAll().stream()
				.map(AdminExperimentMissionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminExperimentMissionResponse get(UUID id) {
		return AdminExperimentMissionResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminExperimentMissionResponse create(AdminExperimentMissionCreateRequest request) {
		experimentMissionRepository.findByCode(request.code()).ifPresent(existing -> {
			throw new BusinessException(ExperimentErrorCode.MISSION_CODE_DUPLICATE);
		});
		ExperimentTopic topic = findTopicOrThrow(request.topicId());
		ExperimentMission mission = ExperimentMission.create(
				request.code(),
				topic,
				request.title(),
				request.description(),
				request.instruction(),
				request.missionType(),
				request.responseType(),
				request.estimatedMinutes(),
				request.emotionalLoad(),
				request.reflectionQuestions(),
				request.examples(),
				request.responseSchema(),
				request.safetyNote(),
				request.active());
		return AdminExperimentMissionResponse.from(experimentMissionRepository.save(mission));
	}

	@Transactional
	public AdminExperimentMissionResponse update(UUID id, AdminExperimentMissionUpdateRequest request) {
		ExperimentMission mission = findOrThrow(id);
		ExperimentTopic topic = findTopicOrThrow(request.topicId());
		mission.update(
				topic,
				request.title(),
				request.description(),
				request.instruction(),
				request.missionType(),
				request.responseType(),
				request.estimatedMinutes(),
				request.emotionalLoad(),
				request.reflectionQuestions(),
				request.examples(),
				request.responseSchema(),
				request.safetyNote(),
				request.active());
		return AdminExperimentMissionResponse.from(mission);
	}

	private ExperimentMission findOrThrow(UUID id) {
		return experimentMissionRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
	}

	private ExperimentTopic findTopicOrThrow(UUID topicId) {
		return experimentTopicRepository.findById(topicId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.TOPIC_NOT_FOUND));
	}

}
