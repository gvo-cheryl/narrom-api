package com.naroom.api.admin.experiment;

import com.naroom.api.admin.experiment.dto.AdminExperimentTopicCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicUpdateRequest;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AdminExperimentTopicService {

	private final ExperimentTopicRepository experimentTopicRepository;

	public AdminExperimentTopicService(ExperimentTopicRepository experimentTopicRepository) {
		this.experimentTopicRepository = experimentTopicRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminExperimentTopicResponse> list() {
		return experimentTopicRepository.findAll().stream()
				.sorted(Comparator.comparingInt(ExperimentTopic::getDisplayOrder))
				.map(AdminExperimentTopicResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminExperimentTopicResponse get(UUID id) {
		return AdminExperimentTopicResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminExperimentTopicResponse create(AdminExperimentTopicCreateRequest request) {
		experimentTopicRepository.findByCode(request.code()).ifPresent(existing -> {
			throw new BusinessException(ExperimentErrorCode.TOPIC_CODE_DUPLICATE);
		});
		ExperimentTopic topic = ExperimentTopic.create(
				request.code(), request.name(), request.description(), request.displayOrder(), request.active());
		return AdminExperimentTopicResponse.from(experimentTopicRepository.save(topic));
	}

	@Transactional
	public AdminExperimentTopicResponse update(UUID id, AdminExperimentTopicUpdateRequest request) {
		ExperimentTopic topic = findOrThrow(id);
		topic.update(request.name(), request.description(), request.displayOrder(), request.active());
		return AdminExperimentTopicResponse.from(topic);
	}

	private ExperimentTopic findOrThrow(UUID id) {
		return experimentTopicRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.TOPIC_NOT_FOUND));
	}

}
