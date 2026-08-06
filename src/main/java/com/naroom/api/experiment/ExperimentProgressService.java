package com.naroom.api.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionRecord;
import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.UserProgramMission;
import com.naroom.api.experiment.domain.entity.UserProgramMissionReplacement;
import com.naroom.api.experiment.domain.entity.UserProgramMissionSlotStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionReplacementRepository;
import com.naroom.api.experiment.dto.ExperimentActiveProgramResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordRequest;
import com.naroom.api.experiment.dto.ExperimentMissionRecordResponse;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceRequest;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceResponse;
import com.naroom.api.experiment.dto.ExperimentProgramDayResponse;
import com.naroom.api.experiment.dto.ExperimentProgramMissionsResponse;
import com.naroom.api.experiment.dto.ExperimentRestedDateResponse;
import com.naroom.api.experiment.dto.ExperimentUserProgramMissionResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagSource;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

// §5.3 진행 중 흐름: 오늘의 작은 실험 조회, §13 미션 기록·교체 트랜잭션을 담당한다. 코스 시작(8-C)과
// 코스 종료·돌아보기(8-E)는 각각 ExperimentEnrollmentService/이후 단계에서 다룬다.
@Service
@Transactional(readOnly = true)
public class ExperimentProgressService {

	private static final Set<UserExperimentProgramStatus> ACTIVE_STATUSES = Set.of(
			UserExperimentProgramStatus.IN_PROGRESS,
			UserExperimentProgramStatus.PAUSED,
			UserExperimentProgramStatus.AWAITING_REVIEW);

	// 이 프로젝트의 Jackson 런타임 설정(Boot 4 기본 Jackson 3 메시지 컨버터)은 ObjectMapper를 스프링
	// 빈으로 노출하지 않는다 - responseData/emotion_data처럼 미션마다 형태가 다른 자유 JSON(§12.3)을
	// 저장할 때만 쓰는 독립 인스턴스다.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final MemberRepository memberRepository;
	private final UserExperimentProgramRepository userExperimentProgramRepository;
	private final UserProgramMissionRepository userProgramMissionRepository;
	private final ExperimentMissionRepository experimentMissionRepository;
	private final ExperimentMissionRecordRepository experimentMissionRecordRepository;
	private final UserProgramMissionReplacementRepository userProgramMissionReplacementRepository;
	private final EntryRepository entryRepository;
	private final EntryTagRepository entryTagRepository;
	private final TagRepository tagRepository;

	public ExperimentProgressService(
			MemberRepository memberRepository,
			UserExperimentProgramRepository userExperimentProgramRepository,
			UserProgramMissionRepository userProgramMissionRepository,
			ExperimentMissionRepository experimentMissionRepository,
			ExperimentMissionRecordRepository experimentMissionRecordRepository,
			UserProgramMissionReplacementRepository userProgramMissionReplacementRepository,
			EntryRepository entryRepository,
			EntryTagRepository entryTagRepository,
			TagRepository tagRepository) {
		this.memberRepository = memberRepository;
		this.userExperimentProgramRepository = userExperimentProgramRepository;
		this.userProgramMissionRepository = userProgramMissionRepository;
		this.experimentMissionRepository = experimentMissionRepository;
		this.experimentMissionRecordRepository = experimentMissionRecordRepository;
		this.userProgramMissionReplacementRepository = userProgramMissionReplacementRepository;
		this.entryRepository = entryRepository;
		this.entryTagRepository = entryTagRepository;
		this.tagRepository = tagRepository;
	}

	public Optional<ExperimentActiveProgramResponse> getActive(UUID memberId) {
		return userExperimentProgramRepository.findByMember_IdAndStatusIn(memberId, ACTIVE_STATUSES)
				.map(program -> {
					UserProgramMission currentSlot = userProgramMissionRepository
							.findByUserExperimentProgram_IdAndSlotStatus(program.getId(), UserProgramMissionSlotStatus.CURRENT)
							.orElse(null);
					return new ExperimentActiveProgramResponse(
							program.getId(),
							program.getStatus(),
							program.getTitleSnapshot(),
							program.getDurationDays(),
							program.getCurrentDay(),
							lookedAtMissionCount(program.getId()),
							restedDateCount(program.getId()),
							currentSlot == null ? null : ExperimentUserProgramMissionResponse.from(currentSlot));
				});
	}

	// E10(전체 진행 보기)·E11(남은 미션 바꾸기)·entry 상세 코스 연결에서 쓰는 일차별 미션·기록 목록.
	// RESTED는 슬롯을 소비하지 않으므로(§13/DEC-03) 일차 카드의 record에는 넣지 않고 별도 목록으로
	// 내려준다 - 같은 슬롯이 여러 번 쉬어간 뒤 소비될 수 있어 슬롯당 기록이 1건이 아닐 수 있다.
	public ExperimentProgramMissionsResponse getMissions(UUID memberId, UUID userExperimentProgramId) {
		UserExperimentProgram program = getOwnedProgramOrThrow(memberId, userExperimentProgramId);

		List<ExperimentMissionRecord> records =
				experimentMissionRecordRepository.findByUserExperimentProgram_IdOrderByRecordDateDesc(program.getId());

		Map<UUID, ExperimentMissionRecord> consumingRecordBySlotId = records.stream()
				.filter(record -> record.getAttemptStatus() != ExperimentAttemptStatus.RESTED)
				.collect(Collectors.toMap(
						record -> record.getUserProgramMission().getId(), Function.identity(), (first, second) -> first));

		List<ExperimentProgramDayResponse> days = userProgramMissionRepository
				.findByUserExperimentProgram_IdOrderByDayNumberAsc(program.getId()).stream()
				.map(slot -> ExperimentProgramDayResponse.of(slot, consumingRecordBySlotId.get(slot.getId())))
				.toList();

		List<ExperimentRestedDateResponse> restedDates = records.stream()
				.filter(record -> record.getAttemptStatus() == ExperimentAttemptStatus.RESTED)
				.sorted(Comparator.comparing(ExperimentMissionRecord::getRecordDate))
				.map(ExperimentRestedDateResponse::from)
				.toList();

		return new ExperimentProgramMissionsResponse(days, restedDates);
	}

	@Transactional
	public ExperimentMissionRecordResponse recordMission(
			UUID memberId, UUID userExperimentProgramId, UUID userProgramMissionId, ExperimentMissionRecordRequest request) {
		UserExperimentProgram program = getOwnedProgramOrThrow(memberId, userExperimentProgramId);
		if (program.getStatus() != UserExperimentProgramStatus.IN_PROGRESS) {
			throw new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_IN_PROGRESS);
		}
		UserProgramMission slot = userProgramMissionRepository
				.findByIdAndUserExperimentProgram_Id(userProgramMissionId, userExperimentProgramId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
		if (slot.getSlotStatus() != UserProgramMissionSlotStatus.CURRENT) {
			throw new BusinessException(ExperimentErrorCode.MISSION_NOT_CURRENT);
		}

		if (request.attemptStatus() == ExperimentAttemptStatus.RESTED) {
			return recordRest(program, slot, request);
		}
		return recordAttempt(memberId, program, slot, request);
	}

	private ExperimentMissionRecordResponse recordRest(
			UserExperimentProgram program, UserProgramMission slot, ExperimentMissionRecordRequest request) {
		boolean alreadyRestedToday = experimentMissionRecordRepository
				.findByUserExperimentProgram_IdAndRecordDateAndAttemptStatus(
						program.getId(), request.recordDate(), ExperimentAttemptStatus.RESTED)
				.isPresent();
		if (alreadyRestedToday) {
			throw new BusinessException(ExperimentErrorCode.ALREADY_RESTED_TODAY);
		}

		experimentMissionRecordRepository.save(
				ExperimentMissionRecord.rest(program, slot, request.recordDate(), request.reflection()));
		program.touchActivity(request.recordDate());

		return new ExperimentMissionRecordResponse(
				ExperimentAttemptStatus.RESTED, false, program.getStatus(), program.getCurrentDay(), true,
				lookedAtMissionCount(program.getId()), restedDateCount(program.getId()));
	}

	private ExperimentMissionRecordResponse recordAttempt(
			UUID memberId, UserExperimentProgram program, UserProgramMission slot, ExperimentMissionRecordRequest request) {
		Entry entry = Boolean.TRUE.equals(request.createLifeTimeEntry())
				? createLifeTimeEntry(memberId, slot, request)
				: null;

		experimentMissionRecordRepository.save(ExperimentMissionRecord.attempt(
				program, slot, request.recordDate(), request.attemptStatus(), request.responseText(),
				toJson(request.responseData()), emotionSnapshot(request.emotionTagIds()),
				request.energyLevel(), request.reflection(), entry));

		Instant now = Instant.now();
		slot.recordAttempt(now);

		boolean isLastDay = slot.getDayNumber() == program.getDurationDays();
		if (isLastDay) {
			program.markAwaitingReview(now);
		} else {
			short nextDayNumber = (short) (slot.getDayNumber() + 1);
			UserProgramMission nextSlot = userProgramMissionRepository
					.findByUserExperimentProgram_IdAndDayNumber(program.getId(), nextDayNumber)
					.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
			nextSlot.promoteToCurrent(now);
			program.advanceDay(request.recordDate());
		}

		return new ExperimentMissionRecordResponse(
				request.attemptStatus(), true, program.getStatus(), program.getCurrentDay(), false,
				lookedAtMissionCount(program.getId()), restedDateCount(program.getId()));
	}

	private Entry createLifeTimeEntry(UUID memberId, UserProgramMission slot, ExperimentMissionRecordRequest request) {
		Member member = memberRepository.getReferenceById(memberId);
		Entry entry = entryRepository.save(Entry.create(
				member, EntryType.EXPERIMENT_MISSION, slot.getTitleSnapshot(), request.responseText(),
				request.recordDate(), null, null, null));
		entry.publish();
		for (UUID tagId : request.emotionTagIds()) {
			Tag tag = tagRepository.findById(tagId)
					.orElseThrow(() -> new BusinessException(RecordErrorCode.TAG_NOT_FOUND));
			entryTagRepository.save(EntryTag.attachSystem(entry, tag, TagSource.EXPERIMENT));
		}
		return entry;
	}

	@Transactional
	public ExperimentMissionReplaceResponse replaceMission(
			UUID memberId, UUID userExperimentProgramId, UUID userProgramMissionId, ExperimentMissionReplaceRequest request) {
		UserExperimentProgram program = getOwnedProgramOrThrow(memberId, userExperimentProgramId);
		UserProgramMission slot = userProgramMissionRepository
				.findByIdAndUserExperimentProgram_Id(userProgramMissionId, userExperimentProgramId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
		if (slot.getSlotStatus() == UserProgramMissionSlotStatus.RECORDED) {
			throw new BusinessException(ExperimentErrorCode.MISSION_ALREADY_RECORDED);
		}

		ExperimentMission replacementMission = experimentMissionRepository.findById(request.replacementMissionId())
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.MISSION_NOT_FOUND));
		if (!replacementMission.isActive()) {
			throw new BusinessException(ExperimentErrorCode.MISSION_INACTIVE);
		}

		boolean duplicate = userProgramMissionRepository
				.findByUserExperimentProgram_IdOrderByDayNumberAsc(program.getId()).stream()
				.filter(other -> other.getMission() != null)
				.anyMatch(other -> other.getMission().getId().equals(replacementMission.getId()));
		if (duplicate) {
			throw new BusinessException(ExperimentErrorCode.DUPLICATE_MISSION_SELECTION);
		}

		UUID originalMissionId = slot.getOriginalMission() != null ? slot.getOriginalMission().getId() : null;
		userProgramMissionReplacementRepository.save(UserProgramMissionReplacement.of(
				slot, slot.getMission(), replacementMission, request.reasonCode(), request.reasonNote()));
		slot.replaceMission(replacementMission);

		return new ExperimentMissionReplaceResponse(
				slot.getId(), slot.getDayNumber(), originalMissionId, replacementMission.getId(), slot.getReplacementCount());
	}

	private UserExperimentProgram getOwnedProgramOrThrow(UUID memberId, UUID userExperimentProgramId) {
		return userExperimentProgramRepository.findByIdAndMember_Id(userExperimentProgramId, memberId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_FOUND));
	}

	private int lookedAtMissionCount(UUID userExperimentProgramId) {
		return (int) experimentMissionRecordRepository.countByUserExperimentProgram_IdAndAttemptStatusNot(
				userExperimentProgramId, ExperimentAttemptStatus.RESTED);
	}

	private int restedDateCount(UUID userExperimentProgramId) {
		return (int) experimentMissionRecordRepository.countByUserExperimentProgram_IdAndAttemptStatus(
				userExperimentProgramId, ExperimentAttemptStatus.RESTED);
	}

	private String emotionSnapshot(List<UUID> emotionTagIds) {
		return emotionTagIds.isEmpty() ? null : toJson(new EmotionSnapshot(emotionTagIds));
	}

	private String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("실험 미션 기록의 JSON 데이터를 직렬화할 수 없습니다.", e);
		}
	}

	private record EmotionSnapshot(List<UUID> emotionTagIds) {
	}

}
