package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentSourceType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.UserProgramMissionSlotStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionRepository;
import com.naroom.api.experiment.dto.ExperimentMissionOverride;
import com.naroom.api.experiment.dto.ExperimentProgramSaveResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedMissionRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramRequest;
import com.naroom.api.experiment.dto.ExperimentUserComposedProgramResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드(NOW_MIND_3, FIND_START_WAY_7 등)가 이미 적용돼 있다는 전제로 §13 코스 시작 트랜잭션을 검증한다.
@SpringBootTest
@Transactional
class ExperimentEnrollmentServiceTest {

	@Autowired
	private ExperimentEnrollmentService experimentEnrollmentService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ExperimentProgramRepository experimentProgramRepository;

	@Autowired
	private ExperimentMissionRepository experimentMissionRepository;

	@Autowired
	private UserExperimentProgramRepository userExperimentProgramRepository;

	@Autowired
	private UserProgramMissionRepository userProgramMissionRepository;

	@Test
	void startFromTemplate_createsInProgressCourseWithDayOneCurrent() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();

		ExperimentProgramStartResponse response = experimentEnrollmentService.startFromTemplate(
				member.getId(), program.getId(), emptyStartRequest());

		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, response.status());
		assertEquals(1, response.currentDay());
		assertEquals(1, response.todayMission().dayNumber());
		assertEquals(
				UserProgramMissionSlotStatus.CURRENT,
				userProgramMissionRepository.findByUserExperimentProgram_IdAndDayNumber(response.userExperimentProgramId(), (short) 1)
						.orElseThrow().getSlotStatus());
	}

	@Test
	void startFromTemplate_missionOverride_replacesThatDaysMission() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = sevenDayProgram();
		ExperimentMission overrideMission = experimentMissionRepository.findByActiveTrue().stream()
				.filter(mission -> !mission.getTopic().getId().equals(program.getPrimaryTopic().getId()))
				.findFirst()
				.orElseThrow();

		ExperimentProgramStartRequest request = new ExperimentProgramStartRequest(
				null, List.of(new ExperimentMissionOverride((short) 2, overrideMission.getId())), null, false);
		ExperimentProgramStartResponse response =
				experimentEnrollmentService.startFromTemplate(member.getId(), program.getId(), request);

		var daySlots = userProgramMissionRepository
				.findByUserExperimentProgram_IdOrderByDayNumberAsc(response.userExperimentProgramId());
		var daySlot2 = daySlots.stream().filter(slot -> slot.getDayNumber() == 2).findFirst().orElseThrow();
		assertEquals(overrideMission.getId(), daySlot2.getMission().getId());
	}

	@Test
	void startFromTemplate_activeCourseExistsWithoutReplace_throwsActiveProgramExists() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();
		experimentEnrollmentService.startFromTemplate(member.getId(), program.getId(), emptyStartRequest());

		ExperimentProgram other = sevenDayProgram();
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentEnrollmentService.startFromTemplate(member.getId(), other.getId(), emptyStartRequest()));

		assertEquals(ExperimentErrorCode.ACTIVE_PROGRAM_EXISTS, exception.errorCode());
	}

	@Test
	void startFromTemplate_activeCourseExistsWithReplace_endsPreviousCourseEarly() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();
		ExperimentProgramStartResponse first =
				experimentEnrollmentService.startFromTemplate(member.getId(), program.getId(), emptyStartRequest());

		ExperimentProgram other = sevenDayProgram();
		ExperimentProgramStartRequest replaceRequest = new ExperimentProgramStartRequest(null, List.of(), null, true);
		ExperimentProgramStartResponse second =
				experimentEnrollmentService.startFromTemplate(member.getId(), other.getId(), replaceRequest);

		assertNotEquals(first.userExperimentProgramId(), second.userExperimentProgramId());
		assertEquals(
				UserExperimentProgramStatus.ENDED_EARLY,
				userExperimentProgramRepository.findById(first.userExperimentProgramId()).orElseThrow().getStatus());
		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, second.status());
	}

	@Test
	void saveFromTemplate_keepsCourseReadyWithoutPromotingDayOne() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();

		ExperimentProgramSaveResponse response =
				experimentEnrollmentService.saveFromTemplate(member.getId(), program.getId(), emptyStartRequest());

		UserExperimentProgram saved = userExperimentProgramRepository.findById(response.userExperimentProgramId()).orElseThrow();
		assertEquals(UserExperimentProgramStatus.READY, saved.getStatus());
		assertEquals(
				UserProgramMissionSlotStatus.PENDING,
				userProgramMissionRepository.findByUserExperimentProgram_IdAndDayNumber(saved.getId(), (short) 1)
						.orElseThrow().getSlotStatus());
	}

	@Test
	void createUserComposed_createsReadyCourseWithSnapshotMissions() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentUserComposedProgramRequest request = userComposedThreeDayRequest();

		ExperimentUserComposedProgramResponse response = experimentEnrollmentService.createUserComposed(member.getId(), request);

		assertEquals(ExperimentSourceType.USER_COMPOSED, response.configurationSource());
		assertEquals(UserExperimentProgramStatus.READY, response.status());
		assertEquals(3, response.missionCount());
		assertEquals(3, userProgramMissionRepository
				.findByUserExperimentProgram_IdOrderByDayNumberAsc(response.userExperimentProgramId()).size());
	}

	@Test
	void createUserComposed_missionCountDoesNotMatchDuration_throwsInvalidMissionCount() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentUserComposedProgramRequest request = new ExperimentUserComposedProgramRequest(
				"짧게 만든 코스", (short) 3, List.of(userComposedMission((short) 1)));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentEnrollmentService.createUserComposed(member.getId(), request));

		assertEquals(ExperimentErrorCode.INVALID_MISSION_COUNT, exception.errorCode());
	}

	@Test
	void createUserComposed_duplicateDayNumber_throwsInvalidDayNumber() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentUserComposedProgramRequest request = new ExperimentUserComposedProgramRequest(
				"중복 일차 코스", (short) 3,
				List.of(userComposedMission((short) 1), userComposedMission((short) 1), userComposedMission((short) 3)));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentEnrollmentService.createUserComposed(member.getId(), request));

		assertEquals(ExperimentErrorCode.INVALID_DAY_NUMBER, exception.errorCode());
	}

	@Test
	void startFromRandom_createsInProgressCourseWithDistinctMissions() {
		Member member = memberRepository.save(Member.create("지연"));

		ExperimentProgramStartResponse response = experimentEnrollmentService.startFromRandom(
				member.getId(), new ExperimentRandomProgramStartRequest((short) 3, false));

		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, response.status());
		assertEquals(3, response.durationDays());
		var slots = userProgramMissionRepository
				.findByUserExperimentProgram_IdOrderByDayNumberAsc(response.userExperimentProgramId());
		assertEquals(3, slots.size());
		assertEquals(3, slots.stream().map(slot -> slot.getMission().getId()).distinct().count());
	}

	@Test
	void activateSaved_promotesReadyCourseToInProgress() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();
		ExperimentProgramSaveResponse saved =
				experimentEnrollmentService.saveFromTemplate(member.getId(), program.getId(), emptyStartRequest());

		ExperimentProgramStartResponse response =
				experimentEnrollmentService.activateSaved(member.getId(), saved.userExperimentProgramId(), false);

		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, response.status());
		assertEquals(1, response.todayMission().dayNumber());
	}

	@Test
	void activateSaved_courseAlreadyInProgress_throwsUserProgramNotReady() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = threeDayProgram();
		ExperimentProgramStartResponse started =
				experimentEnrollmentService.startFromTemplate(member.getId(), program.getId(), emptyStartRequest());

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentEnrollmentService.activateSaved(member.getId(), started.userExperimentProgramId(), false));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_NOT_READY, exception.errorCode());
	}

	@Test
	void activateSaved_unknownId_throwsUserProgramNotFound() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentEnrollmentService.activateSaved(member.getId(), UUID.randomUUID(), false));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_NOT_FOUND, exception.errorCode());
	}

	private ExperimentProgram threeDayProgram() {
		return experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
	}

	private ExperimentProgram sevenDayProgram() {
		return experimentProgramRepository.findByCode("FIND_START_WAY_7").orElseThrow();
	}

	private ExperimentProgramStartRequest emptyStartRequest() {
		return new ExperimentProgramStartRequest(null, List.of(), null, false);
	}

	private ExperimentUserComposedProgramRequest userComposedThreeDayRequest() {
		return new ExperimentUserComposedProgramRequest(
				"퇴근 뒤 마음 정리해보기", (short) 3,
				List.of(userComposedMission((short) 1), userComposedMission((short) 2), userComposedMission((short) 3)));
	}

	private ExperimentUserComposedMissionRequest userComposedMission(short dayNumber) {
		return new ExperimentUserComposedMissionRequest(
				dayNumber, "직접 만든 미션 " + dayNumber, "직접 적은 안내문", com.naroom.api.experiment.domain.entity.ExperimentMissionType.OBSERVATION,
				(short) 3);
	}

}
