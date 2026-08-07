package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.badge.BadgeAwardService;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendation;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.UserProgramMission;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.ExperimentRecommendationRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionRepository;
import com.naroom.api.experiment.dto.ExperimentMissionOverride;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSaveResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedMissionRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramResponse;
import com.naroom.api.experiment.dto.ExperimentUserProgramMissionResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// §13 코스 시작 트랜잭션(TEMPLATE/RANDOM/USER_COMPOSED 시작·저장, 저장해둔 코스 지금 시작하기)을 담당한다.
// 미션 기록·교체·휴식·종료·회고는 각각 8-D, 8-E 범위다.
@Service
@Transactional(readOnly = true)
public class ExperimentEnrollmentService {

	// DEC-02: 활성 상태는 회원당 1개다(uq_user_experiment_one_active와 동일한 상태 집합).
	private static final Set<UserExperimentProgramStatus> ACTIVE_STATUSES = Set.of(
			UserExperimentProgramStatus.IN_PROGRESS,
			UserExperimentProgramStatus.PAUSED,
			UserExperimentProgramStatus.AWAITING_REVIEW);

	private final MemberRepository memberRepository;
	private final ExperimentProgramRepository experimentProgramRepository;
	private final ExperimentProgramMissionRepository experimentProgramMissionRepository;
	private final ExperimentMissionRepository experimentMissionRepository;
	private final UserExperimentProgramRepository userExperimentProgramRepository;
	private final UserProgramMissionRepository userProgramMissionRepository;
	private final ExperimentRecommendationRepository experimentRecommendationRepository;
	private final ExperimentRandomProgramComposer experimentRandomProgramComposer;
	private final BadgeAwardService badgeAwardService;

	public ExperimentEnrollmentService(
			MemberRepository memberRepository,
			ExperimentProgramRepository experimentProgramRepository,
			ExperimentProgramMissionRepository experimentProgramMissionRepository,
			ExperimentMissionRepository experimentMissionRepository,
			UserExperimentProgramRepository userExperimentProgramRepository,
			UserProgramMissionRepository userProgramMissionRepository,
			ExperimentRecommendationRepository experimentRecommendationRepository,
			ExperimentRandomProgramComposer experimentRandomProgramComposer,
			BadgeAwardService badgeAwardService) {
		this.memberRepository = memberRepository;
		this.experimentProgramRepository = experimentProgramRepository;
		this.experimentProgramMissionRepository = experimentProgramMissionRepository;
		this.experimentMissionRepository = experimentMissionRepository;
		this.userExperimentProgramRepository = userExperimentProgramRepository;
		this.userProgramMissionRepository = userProgramMissionRepository;
		this.experimentRecommendationRepository = experimentRecommendationRepository;
		this.experimentRandomProgramComposer = experimentRandomProgramComposer;
		this.badgeAwardService = badgeAwardService;
	}

	@Transactional
	public ExperimentProgramStartResponse startFromTemplate(UUID memberId, UUID programId, ExperimentProgramStartRequest request) {
		resolveActiveConflict(memberId, request.replaceActiveProgram());
		ExperimentProgram program = experimentProgramRepository.findById(programId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.PROGRAM_NOT_FOUND));
		if (request.expectedContentVersion() != null && !request.expectedContentVersion().equals(program.getContentVersion())) {
			throw new BusinessException(ExperimentErrorCode.CONTENT_VERSION_MISMATCH);
		}

		Member member = memberRepository.getReferenceById(memberId);
		UserExperimentProgram userProgram = userExperimentProgramRepository.save(UserExperimentProgram.ready(
				member, program, program.getContentVersion(), ExperimentSourceType.TEMPLATE,
				program.getTitle(), program.getDescription(), program.getDurationDays()));

		Map<Short, UUID> missionIdByDay = resolveMissionIdsByDay(
				experimentProgramMissionRepository.findByProgram_IdOrderById_DayNumberAsc(programId),
				request.missionOverrides(), program.getDurationDays());
		createSnapshotSlotsFromCatalog(userProgram, missionIdByDay);

		Instant now = Instant.now();
		UserProgramMission today = promoteDayOneToCurrent(userProgram, now);
		userProgram.activate(now);
		acceptRecommendationIfPresent(memberId, request.recommendationId(), userProgram, now);
		badgeAwardService.award(memberId, BadgeCode.FIRST_EXPERIMENT_START);

		return toStartResponse(userProgram, today);
	}

	@Transactional
	public ExperimentProgramSaveResponse saveFromTemplate(UUID memberId, UUID programId, ExperimentProgramStartRequest request) {
		ExperimentProgram program = experimentProgramRepository.findById(programId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.PROGRAM_NOT_FOUND));
		if (request.expectedContentVersion() != null && !request.expectedContentVersion().equals(program.getContentVersion())) {
			throw new BusinessException(ExperimentErrorCode.CONTENT_VERSION_MISMATCH);
		}

		Member member = memberRepository.getReferenceById(memberId);
		UserExperimentProgram userProgram = userExperimentProgramRepository.save(UserExperimentProgram.ready(
				member, program, program.getContentVersion(), ExperimentSourceType.TEMPLATE,
				program.getTitle(), program.getDescription(), program.getDurationDays()));

		Map<Short, UUID> missionIdByDay = resolveMissionIdsByDay(
				experimentProgramMissionRepository.findByProgram_IdOrderById_DayNumberAsc(programId),
				request.missionOverrides(), program.getDurationDays());
		createSnapshotSlotsFromCatalog(userProgram, missionIdByDay);

		return new ExperimentProgramSaveResponse(
				userProgram.getId(), userProgram.getStatus(), userProgram.getTitleSnapshot(), userProgram.getDurationDays());
	}

	@Transactional
	public ExperimentUserComposedProgramResponse createUserComposed(UUID memberId, ExperimentUserComposedProgramRequest request) {
		if (request.durationDays() != 3 && request.durationDays() != 7) {
			throw new BusinessException(ExperimentErrorCode.DURATION_INVALID);
		}
		if (request.missions().size() != request.durationDays()) {
			throw new BusinessException(ExperimentErrorCode.INVALID_MISSION_COUNT);
		}
		Set<Short> dayNumbers = new HashSet<>();
		for (ExperimentUserComposedMissionRequest mission : request.missions()) {
			if (mission.dayNumber() < 1 || mission.dayNumber() > request.durationDays() || !dayNumbers.add(mission.dayNumber())) {
				throw new BusinessException(ExperimentErrorCode.INVALID_DAY_NUMBER);
			}
		}

		Member member = memberRepository.getReferenceById(memberId);
		UserExperimentProgram userProgram = userExperimentProgramRepository.save(UserExperimentProgram.ready(
				member, null, null, ExperimentSourceType.USER_COMPOSED,
				request.title(), "", request.durationDays()));

		for (ExperimentUserComposedMissionRequest mission : request.missions()) {
			userProgramMissionRepository.save(UserProgramMission.userComposed(
					userProgram, mission.dayNumber(), mission.title(), mission.instruction(),
					mission.missionType(), mission.estimatedMinutes()));
		}

		return new ExperimentUserComposedProgramResponse(
				userProgram.getId(), null, ExperimentSourceType.USER_COMPOSED, userProgram.getStatus(),
				userProgram.getDurationDays(), request.missions().size());
	}

	@Transactional
	public ExperimentProgramStartResponse startFromRandom(UUID memberId, ExperimentRandomProgramStartRequest request) {
		resolveActiveConflict(memberId, request.replaceActiveProgram());

		List<ExperimentProgramMissionResponse> composed =
				experimentRandomProgramComposer.compose(memberId, request.durationDays()).missions();

		Member member = memberRepository.getReferenceById(memberId);
		String title = "무작위로 고른 " + request.durationDays() + "일 코스";
		UserExperimentProgram userProgram = userExperimentProgramRepository.save(UserExperimentProgram.ready(
				member, null, null, ExperimentSourceType.RANDOM, title, "", request.durationDays()));

		for (ExperimentProgramMissionResponse missionResponse : composed) {
			ExperimentMission mission = experimentMissionRepository.findById(missionResponse.missionId())
					.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
			userProgramMissionRepository.save(UserProgramMission.fromCatalog(userProgram, missionResponse.dayNumber(), mission));
		}

		Instant now = Instant.now();
		UserProgramMission today = promoteDayOneToCurrent(userProgram, now);
		userProgram.activate(now);
		badgeAwardService.award(memberId, BadgeCode.FIRST_EXPERIMENT_START);

		return toStartResponse(userProgram, today);
	}

	@Transactional
	public ExperimentProgramStartResponse activateSaved(UUID memberId, UUID userExperimentProgramId, boolean replaceActiveProgram) {
		UserExperimentProgram userProgram = userExperimentProgramRepository.findByIdAndMember_Id(userExperimentProgramId, memberId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_FOUND));
		if (userProgram.getStatus() != UserExperimentProgramStatus.READY) {
			throw new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_READY);
		}
		resolveActiveConflict(memberId, replaceActiveProgram);

		Instant now = Instant.now();
		UserProgramMission today = promoteDayOneToCurrent(userProgram, now);
		userProgram.activate(now);
		badgeAwardService.award(memberId, BadgeCode.FIRST_EXPERIMENT_START);

		return toStartResponse(userProgram, today);
	}

	// §5.5 활성 코스 충돌: replaceActiveProgram이 false면 막고, true면 "여기까지 기록하고 새 코스로 바꾸기"를
	// 적용해 기존 활성 코스를 ENDED_EARLY로 종료한다. 기존 기록은 지우지 않는다.
	private void resolveActiveConflict(UUID memberId, boolean replaceActiveProgram) {
		userExperimentProgramRepository.findByMember_IdAndStatusIn(memberId, ACTIVE_STATUSES).ifPresent(active -> {
			if (!replaceActiveProgram) {
				throw new BusinessException(ExperimentErrorCode.ACTIVE_PROGRAM_EXISTS);
			}
			active.endEarly(Instant.now());
		});
	}

	private Map<Short, UUID> resolveMissionIdsByDay(
			List<ExperimentProgramMission> catalogMissions, List<ExperimentMissionOverride> overrides, short durationDays) {
		Map<Short, UUID> missionIdByDay = new HashMap<>();
		for (ExperimentProgramMission catalogMission : catalogMissions) {
			missionIdByDay.put(catalogMission.getDayNumber(), catalogMission.getMission().getId());
		}
		for (ExperimentMissionOverride override : overrides) {
			if (override.dayNumber() < 1 || override.dayNumber() > durationDays) {
				throw new BusinessException(ExperimentErrorCode.INVALID_DAY_NUMBER);
			}
			missionIdByDay.put(override.dayNumber(), override.missionId());
		}
		if (new HashSet<>(missionIdByDay.values()).size() != missionIdByDay.size()) {
			throw new BusinessException(ExperimentErrorCode.DUPLICATE_MISSION_SELECTION);
		}
		return missionIdByDay;
	}

	private void createSnapshotSlotsFromCatalog(UserExperimentProgram userProgram, Map<Short, UUID> missionIdByDay) {
		for (Map.Entry<Short, UUID> entry : missionIdByDay.entrySet()) {
			ExperimentMission mission = experimentMissionRepository.findById(entry.getValue())
					.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
			userProgramMissionRepository.save(UserProgramMission.fromCatalog(userProgram, entry.getKey(), mission));
		}
	}

	private UserProgramMission promoteDayOneToCurrent(UserExperimentProgram userProgram, Instant now) {
		UserProgramMission dayOne = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(userProgram.getId(), (short) 1)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
		dayOne.promoteToCurrent(now);
		return dayOne;
	}

	// recommendationId는 참고 정보다 - 존재하지 않거나 다른 회원의 것이면 시작 자체는 막지 않고 넘어간다.
	private void acceptRecommendationIfPresent(
			UUID memberId, UUID recommendationId, UserExperimentProgram userProgram, Instant now) {
		if (recommendationId == null) {
			return;
		}
		experimentRecommendationRepository.findByIdAndMember_Id(recommendationId, memberId)
				.ifPresent((ExperimentRecommendation recommendation) -> recommendation.accept(userProgram, now));
	}

	private ExperimentProgramStartResponse toStartResponse(UserExperimentProgram userProgram, UserProgramMission today) {
		return new ExperimentProgramStartResponse(
				userProgram.getId(),
				userProgram.getStatus(),
				userProgram.getTitleSnapshot(),
				userProgram.getDurationDays(),
				userProgram.getCurrentDay(),
				0,
				0,
				ExperimentUserProgramMissionResponse.from(today));
	}

}
