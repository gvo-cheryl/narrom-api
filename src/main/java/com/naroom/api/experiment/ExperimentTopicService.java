package com.naroom.api.experiment;

import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import com.naroom.api.experiment.dto.ExperimentTopicResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExperimentTopicService {

	private final ExperimentTopicRepository experimentTopicRepository;

	public ExperimentTopicService(ExperimentTopicRepository experimentTopicRepository) {
		this.experimentTopicRepository = experimentTopicRepository;
	}

	public List<ExperimentTopicResponse> listActive() {
		return experimentTopicRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
				.map(ExperimentTopicResponse::from)
				.toList();
	}

}
