package com.naroom.api.admin.experiment;

import com.naroom.api.admin.common.AdminCodeGenerator;
import com.naroom.api.admin.common.AdminSearchSpecifications;
import com.naroom.api.admin.common.AdminSortParser;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramCreateRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramDayMissionRequest;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramResponse;
import com.naroom.api.admin.experiment.dto.AdminExperimentProgramUpdateRequest;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.ExperimentTopic;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.ExperimentTopicRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminExperimentProgramService {

	private static final Set<Short> ALLOWED_DURATIONS = Set.of((short) 3, (short) 7);

	// docs/contracts/drafts/admin-list-search-sort.md 권장안.
	private static final List<String> SEARCH_FIELDS = List.of("title", "code", "description");
	private static final Set<String> SORTABLE_FIELDS = Set.of("code", "title", "status", "updatedAt", "createdAt");
	private static final Sort DEFAULT_SORT =
			Sort.by(Sort.Direction.ASC, "code").and(Sort.by(Sort.Direction.DESC, "contentVersion"));

	private final ExperimentProgramRepository experimentProgramRepository;
	private final ExperimentProgramMissionRepository experimentProgramMissionRepository;
	private final ExperimentMissionRepository experimentMissionRepository;
	private final ExperimentTopicRepository experimentTopicRepository;

	public AdminExperimentProgramService(
			ExperimentProgramRepository experimentProgramRepository,
			ExperimentProgramMissionRepository experimentProgramMissionRepository,
			ExperimentMissionRepository experimentMissionRepository,
			ExperimentTopicRepository experimentTopicRepository) {
		this.experimentProgramRepository = experimentProgramRepository;
		this.experimentProgramMissionRepository = experimentProgramMissionRepository;
		this.experimentMissionRepository = experimentMissionRepository;
		this.experimentTopicRepository = experimentTopicRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminExperimentProgramResponse> list(String q, String sort) {
		Specification<ExperimentProgram> specification = AdminSearchSpecifications.containsAnyIgnoreCase(q, SEARCH_FIELDS);
		Sort resolvedSort = AdminSortParser.parse(sort, SORTABLE_FIELDS, DEFAULT_SORT);
		return experimentProgramRepository.findAll(Specification.where(specification), resolvedSort).stream()
				.map(program -> AdminExperimentProgramResponse.from(program, dayMissionsOf(program.getId())))
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminExperimentProgramResponse get(UUID id) {
		ExperimentProgram program = findOrThrow(id);
		return AdminExperimentProgramResponse.from(program, dayMissionsOf(program.getId()));
	}

	@Transactional
	public AdminExperimentProgramResponse create(AdminExperimentProgramCreateRequest request, UUID actingAdminId) {
		requireAllowedDuration(request.durationDays());
		ExperimentTopic primaryTopic = findTopicOrThrow(request.primaryTopicId());
		ExperimentProgram program = ExperimentProgram.create(
				AdminCodeGenerator.generate("program"), 1, primaryTopic, request.title(), request.description(),
				request.durationDays(), request.sourceType(), request.estimatedMinutesMin(),
				request.estimatedMinutesMax(), request.featured(), request.beginner(), request.displayOrder(), null,
				actingAdminId);
		program = experimentProgramRepository.save(program);
		List<ExperimentProgramMission> programMissions = buildDayMissions(program, request.durationDays(), request.days());
		experimentProgramMissionRepository.saveAll(programMissions);
		return AdminExperimentProgramResponse.from(program, programMissions);
	}

	@Transactional
	public AdminExperimentProgramResponse update(UUID id, AdminExperimentProgramUpdateRequest request) {
		ExperimentProgram program = findOrThrow(id);
		requireStatus(program, ExperimentProgramStatus.DRAFT);
		requireAllowedDuration(request.durationDays());
		ExperimentTopic primaryTopic = findTopicOrThrow(request.primaryTopicId());
		program.updateDraft(
				primaryTopic, request.title(), request.description(), request.durationDays(), request.sourceType(),
				request.estimatedMinutesMin(), request.estimatedMinutesMax(), request.featured(), request.beginner(),
				request.displayOrder());
		experimentProgramMissionRepository.deleteByProgram_Id(program.getId());
		List<ExperimentProgramMission> programMissions = buildDayMissions(program, request.durationDays(), request.days());
		experimentProgramMissionRepository.saveAll(programMissions);
		return AdminExperimentProgramResponse.from(program, programMissions);
	}

	// §19.4 편집 API 버전 규칙: 발행본은 직접 수정하지 않고, 발행본 내용을 시작점으로 하는 새 DRAFT를 만든다.
	@Transactional
	public AdminExperimentProgramResponse createRevision(UUID publishedId, UUID actingAdminId) {
		ExperimentProgram published = findOrThrow(publishedId);
		requireStatus(published, ExperimentProgramStatus.PUBLISHED);
		ExperimentProgram draft = ExperimentProgram.create(
				published.getCode(), published.getContentVersion() + 1, published.getPrimaryTopic(),
				published.getTitle(), published.getDescription(), published.getDurationDays(),
				published.getSourceType(), published.getEstimatedMinutesMin(), published.getEstimatedMinutesMax(),
				published.isFeatured(), published.isBeginner(), published.getDisplayOrder(),
				published.getId(), actingAdminId);
		ExperimentProgram savedDraft = experimentProgramRepository.save(draft);
		List<ExperimentProgramMission> clonedMissions = dayMissionsOf(published.getId()).stream()
				.map(source -> ExperimentProgramMission.of(
						savedDraft, source.getMission(), source.getDayNumber(), source.isReplaceable(),
						source.getReplacementGroup()))
				.toList();
		experimentProgramMissionRepository.saveAll(clonedMissions);
		return AdminExperimentProgramResponse.from(savedDraft, clonedMissions);
	}

	// 같은 code로 이미 PUBLISHED된 버전이 있으면 먼저 ARCHIVED로 내린다(experiment_programs는 code당 PUBLISHED 1개만 허용).
	// §19.5: 프론트의 사전 validate 호출 여부와 무관하게 게시 시점에 전체 검증을 다시 실행한다.
	@Transactional
	public AdminExperimentProgramResponse publish(UUID draftId) {
		ExperimentProgram draft = findOrThrow(draftId);
		requireStatus(draft, ExperimentProgramStatus.DRAFT);
		List<ExperimentProgramMission> programMissions = dayMissionsOf(draft.getId());
		requireAllowedDuration(draft.getDurationDays());
		requireCompleteDayCoverage(draft.getDurationDays(), programMissions.stream()
				.map(ExperimentProgramMission::getDayNumber).collect(Collectors.toSet()));
		requirePublishableMissions(programMissions);
		experimentProgramRepository.findByCodeAndStatus(draft.getCode(), ExperimentProgramStatus.PUBLISHED)
				.ifPresent(ExperimentProgram::archive);
		draft.publish();
		return AdminExperimentProgramResponse.from(draft, programMissions);
	}

	@Transactional
	public AdminExperimentProgramResponse archive(UUID id) {
		ExperimentProgram program = findOrThrow(id);
		requireStatus(program, ExperimentProgramStatus.PUBLISHED);
		program.archive();
		return AdminExperimentProgramResponse.from(program, dayMissionsOf(program.getId()));
	}

	private List<ExperimentProgramMission> buildDayMissions(
			ExperimentProgram program, short durationDays, List<AdminExperimentProgramDayMissionRequest> days) {
		requireCompleteDayCoverage(durationDays, days.stream()
				.map(AdminExperimentProgramDayMissionRequest::dayNumber).collect(Collectors.toSet()));
		if (days.stream().map(AdminExperimentProgramDayMissionRequest::dayNumber).distinct().count() != days.size()) {
			throw new BusinessException(ExperimentErrorCode.INVALID_DAY_NUMBER);
		}
		Set<UUID> missionIds = days.stream().map(AdminExperimentProgramDayMissionRequest::missionId).collect(Collectors.toSet());
		Map<UUID, ExperimentMission> missionsById = experimentMissionRepository.findAllById(missionIds).stream()
				.collect(Collectors.toMap(ExperimentMission::getId, mission -> mission));
		if (missionsById.size() != missionIds.size()) {
			throw new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND);
		}
		return days.stream()
				.map(day -> ExperimentProgramMission.of(
						program, missionsById.get(day.missionId()), day.dayNumber(), day.replaceable(),
						day.replacementGroup()))
				.toList();
	}

	private void requireCompleteDayCoverage(short durationDays, Set<Short> presentDayNumbers) {
		Set<Short> expected = IntStream.rangeClosed(1, durationDays)
				.mapToObj(day -> (short) day)
				.collect(Collectors.toSet());
		if (!expected.equals(presentDayNumbers)) {
			throw new BusinessException(ExperimentErrorCode.INVALID_DAY_NUMBER);
		}
	}

	private void requirePublishableMissions(List<ExperimentProgramMission> programMissions) {
		Map<String, Long> countByReplacementGroup = programMissions.stream()
				.filter(programMission -> programMission.getReplacementGroup() != null)
				.collect(Collectors.groupingBy(ExperimentProgramMission::getReplacementGroup, Collectors.counting()));
		for (ExperimentProgramMission programMission : programMissions) {
			if (!programMission.getMission().isActive()) {
				throw new BusinessException(ExperimentErrorCode.MISSION_INACTIVE);
			}
			if (programMission.isReplaceable()) {
				String group = programMission.getReplacementGroup();
				if (group == null || countByReplacementGroup.getOrDefault(group, 0L) < 2) {
					throw new BusinessException(ExperimentErrorCode.PROGRAM_REPLACEMENT_GROUP_INVALID);
				}
			}
		}
	}

	private void requireAllowedDuration(short durationDays) {
		if (!ALLOWED_DURATIONS.contains(durationDays)) {
			throw new BusinessException(ExperimentErrorCode.DURATION_INVALID);
		}
	}

	private void requireStatus(ExperimentProgram program, ExperimentProgramStatus expected) {
		if (program.getStatus() != expected) {
			throw new BusinessException(
					expected == ExperimentProgramStatus.DRAFT
							? ExperimentErrorCode.PROGRAM_NOT_DRAFT
							: ExperimentErrorCode.PROGRAM_NOT_PUBLISHED);
		}
	}

	private List<ExperimentProgramMission> dayMissionsOf(UUID programId) {
		return experimentProgramMissionRepository.findByProgram_IdOrderById_DayNumberAsc(programId);
	}

	private ExperimentProgram findOrThrow(UUID id) {
		return experimentProgramRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.PROGRAM_NOT_FOUND));
	}

	private ExperimentTopic findTopicOrThrow(UUID topicId) {
		return experimentTopicRepository.findById(topicId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.TOPIC_NOT_FOUND));
	}

}
