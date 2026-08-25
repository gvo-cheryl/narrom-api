package com.naroom.api.admin.experiment;

import com.naroom.api.admin.common.AdminCodeGenerator;
import com.naroom.api.admin.common.AdminSearchSpecifications;
import com.naroom.api.admin.common.AdminSortParser;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentTopicUpdateRequest;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminExperimentTopicService {

	// docs/contracts/drafts/admin-list-search-sort.md 권장안.
	private static final List<String> SEARCH_FIELDS = List.of("name", "code", "description");
	private static final Set<String> SORTABLE_FIELDS =
			Set.of("name", "code", "displayOrder", "active", "updatedAt", "createdAt");
	private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "displayOrder");

	private final ExperimentTopicRepository experimentTopicRepository;

	public AdminExperimentTopicService(ExperimentTopicRepository experimentTopicRepository) {
		this.experimentTopicRepository = experimentTopicRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminExperimentTopicResponse> list(String q, String sort) {
		Specification<ExperimentTopic> specification = AdminSearchSpecifications.containsAnyIgnoreCase(q, SEARCH_FIELDS);
		Sort resolvedSort = AdminSortParser.parse(sort, SORTABLE_FIELDS, DEFAULT_SORT);
		return experimentTopicRepository.findAll(Specification.where(specification), resolvedSort).stream()
				.map(AdminExperimentTopicResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminExperimentTopicResponse get(UUID id) {
		return AdminExperimentTopicResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminExperimentTopicResponse create(AdminExperimentTopicCreateRequest request) {
		ExperimentTopic topic = ExperimentTopic.create(
				AdminCodeGenerator.generate("topic"), request.name(), request.description(), request.displayOrder(),
				request.active());
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
