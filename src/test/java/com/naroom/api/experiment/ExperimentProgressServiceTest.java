package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.UserProgramMission;
import com.naroom.api.experiment.domain.entity.UserProgramMissionSlotStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionRepository;
import com.naroom.api.experiment.dto.ExperimentActiveProgramResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordRequest;
import com.naroom.api.experiment.dto.ExperimentMissionRecordResponse;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceRequest;
import com.naroom.api.experiment.dto.ExperimentMissionReplaceResponse;
import com.naroom.api.experiment.dto.ExperimentProgramMissionsResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드 데이터를 활용해 §13 미션 기록·교체 트랜잭션과 §5.3 진행 중 조회를 검증한다.
@SpringBootTest
@Transactional
class ExperimentProgressServiceTest {

	@Autowired
	private ExperimentProgressService experimentProgressService;

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

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private TagRepository tagRepository;

	@Test
	void getActive_noActiveCourse_returnsEmpty() {
		Member member = memberRepository.save(Member.create("지연"));

		Optional<ExperimentActiveProgramResponse> active = experimentProgressService.getActive(member.getId());

		assertTrue(active.isEmpty());
	}

	@Test
	void getActive_returnsTodayMissionAndCounts() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());

		ExperimentActiveProgramResponse active = experimentProgressService.getActive(member.getId()).orElseThrow();

		assertEquals(started.userExperimentProgramId(), active.userExperimentProgramId());
		assertEquals(1, active.todayMission().dayNumber());
		assertEquals(0, active.lookedAtMissionCount());
		assertEquals(0, active.restedDateCount());
	}

	@Test
	void recordMission_consumingStatus_promotesNextDayAndAdvancesCurrentDay() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());

		ExperimentMissionRecordResponse response = experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false));

		assertTrue(response.missionConsumed());
		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, response.status());
		assertEquals(2, response.currentDay());
		assertEquals(1, response.lookedAtMissionCount());
		UserProgramMission daySlot2 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 2)
				.orElseThrow();
		assertEquals(UserProgramMissionSlotStatus.CURRENT, daySlot2.getSlotStatus());
	}

	@Test
	void recordMission_lastDay_marksAwaitingReview() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		recordDay(member.getId(), started.userExperimentProgramId(), 1, ExperimentAttemptStatus.DONE);
		recordDay(member.getId(), started.userExperimentProgramId(), 2, ExperimentAttemptStatus.DONE);

		UserProgramMission daySlot3 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 3)
				.orElseThrow();
		ExperimentMissionRecordResponse response = experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), daySlot3.getId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false));

		assertEquals(UserExperimentProgramStatus.AWAITING_REVIEW, response.status());
	}

	@Test
	void recordMission_rested_keepsSameSlotCurrentAndDoesNotAdvanceDay() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());

		ExperimentMissionRecordResponse response = experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.RESTED, LocalDate.now(), false));

		assertFalse(response.missionConsumed());
		assertEquals(1, response.currentDay());
		assertTrue(response.sameMissionRemains());
		assertEquals(1, response.restedDateCount());
		UserProgramMission daySlot1 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 1)
				.orElseThrow();
		assertEquals(UserProgramMissionSlotStatus.CURRENT, daySlot1.getSlotStatus());
	}

	@Test
	void recordMission_restedTwiceSameDate_throwsAlreadyRestedToday() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.RESTED, LocalDate.now(), false));

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.RESTED, LocalDate.now(), false)));

		assertEquals(ExperimentErrorCode.ALREADY_RESTED_TODAY, exception.errorCode());
	}

	@Test
	void recordMission_notCurrentSlot_throwsMissionNotCurrent() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		UserProgramMission daySlot2 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 2)
				.orElseThrow();

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), daySlot2.getId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false)));

		assertEquals(ExperimentErrorCode.MISSION_NOT_CURRENT, exception.errorCode());
	}

	@Test
	void recordMission_programNotInProgress_throwsUserProgramNotInProgress() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		var saved = experimentEnrollmentService.saveFromTemplate(
				member.getId(), program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));
		UserProgramMission daySlot1 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(saved.userExperimentProgramId(), (short) 1)
				.orElseThrow();

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentProgressService.recordMission(
				member.getId(), saved.userExperimentProgramId(), daySlot1.getId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false)));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_NOT_IN_PROGRESS, exception.errorCode());
	}

	@Test
	void recordMission_createLifeTimeEntry_createsEntryWithEmotionTag() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		Tag emotionTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "안도감", "안도감" + System.nanoTime()));

		ExperimentMissionRecordRequest request = new ExperimentMissionRecordRequest(
				ExperimentAttemptStatus.DONE, LocalDate.now(), "오늘은 이렇게 해봤다.", null,
				List.of(emotionTag.getId()), (short) 3, "다음엔 다르게 해보고 싶다.", true);

		experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(), request);

		assertEquals(1, entryRepository.findAll().stream()
				.filter(e -> e.getMember().getId().equals(member.getId())).count());
		com.naroom.api.record.domain.entity.Entry createdEntry = entryRepository.findAll().stream()
				.filter(e -> e.getMember().getId().equals(member.getId())).findFirst().orElseThrow();
		assertEquals(1, entryTagRepository.findByEntry_IdAndSource(
				createdEntry.getId(), com.naroom.api.record.domain.entity.TagSource.EXPERIMENT).size());
		assertEquals(started.userExperimentProgramId(), createdEntry.getRelatedExperimentProgramId());
	}

	@Test
	void replaceMission_pendingSlot_replacesMissionAndKeepsOriginalMissionId() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		UserProgramMission daySlot2 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 2)
				.orElseThrow();
		UUID originalMissionId = daySlot2.getMission().getId();
		ExperimentMission replacement = experimentMissionRepository.findByActiveTrue().stream()
				.filter(mission -> !mission.getId().equals(originalMissionId))
				.filter(mission -> !mission.getId().equals(started.todayMission().missionId()))
				.findFirst().orElseThrow();

		ExperimentMissionReplaceResponse response = experimentProgressService.replaceMission(
				member.getId(), started.userExperimentProgramId(), daySlot2.getId(),
				new ExperimentMissionReplaceRequest(replacement.getId(), null, null));

		assertEquals(originalMissionId, response.originalMissionId());
		assertEquals(replacement.getId(), response.missionId());
		assertEquals(1, response.replacementCount());
	}

	@Test
	void replaceMission_recordedSlot_throwsMissionAlreadyRecorded() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false));
		ExperimentMission anyMission = experimentMissionRepository.findByActiveTrue().stream().findFirst().orElseThrow();

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentProgressService.replaceMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				new ExperimentMissionReplaceRequest(anyMission.getId(), null, null)));

		assertEquals(ExperimentErrorCode.MISSION_ALREADY_RECORDED, exception.errorCode());
	}

	@Test
	void replaceMission_missionAlreadyUsedInSameCourse_throwsDuplicateMissionSelection() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		UserProgramMission daySlot2 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 2)
				.orElseThrow();
		UUID day1MissionId = started.todayMission().missionId();

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentProgressService.replaceMission(
				member.getId(), started.userExperimentProgramId(), daySlot2.getId(),
				new ExperimentMissionReplaceRequest(day1MissionId, null, null)));

		assertEquals(ExperimentErrorCode.DUPLICATE_MISSION_SELECTION, exception.errorCode());
	}

	@Test
	void getMissions_afterOneAttempt_marksDay1RecordedWithConsumingRecord() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());

		recordDay(member.getId(), started.userExperimentProgramId(), 1, ExperimentAttemptStatus.DONE);

		ExperimentProgramMissionsResponse missions =
				experimentProgressService.getMissions(member.getId(), started.userExperimentProgramId());

		assertEquals(3, missions.days().size());
		assertEquals(UserProgramMissionSlotStatus.RECORDED, missions.days().get(0).slotStatus());
		assertEquals(ExperimentAttemptStatus.DONE, missions.days().get(0).record().attemptStatus());
		assertEquals(UserProgramMissionSlotStatus.CURRENT, missions.days().get(1).slotStatus());
		assertEquals(null, missions.days().get(1).record());
		assertTrue(missions.restedDates().isEmpty());
	}

	@Test
	void getMissions_afterRest_returnsRestedDateSeparatelyWithoutConsumingSlot() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		UserProgramMission daySlot1 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 1)
				.orElseThrow();

		experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), daySlot1.getId(),
				recordRequest(ExperimentAttemptStatus.RESTED, LocalDate.now(), false));

		ExperimentProgramMissionsResponse missions =
				experimentProgressService.getMissions(member.getId(), started.userExperimentProgramId());

		assertEquals(1, missions.restedDates().size());
		assertEquals((short) 1, missions.restedDates().get(0).dayNumber());
		assertEquals(UserProgramMissionSlotStatus.CURRENT, missions.days().get(0).slotStatus());
		assertEquals(null, missions.days().get(0).record());
	}

	@Test
	void getMissions_afterReplace_marksThatDayReplaced() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		UserProgramMission daySlot2 = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) 2)
				.orElseThrow();
		ExperimentMission replacement = experimentMissionRepository.findByCode("CALM_MOMENT").orElseThrow();

		experimentProgressService.replaceMission(
				member.getId(), started.userExperimentProgramId(), daySlot2.getId(),
				new ExperimentMissionReplaceRequest(replacement.getId(), null, null));

		ExperimentProgramMissionsResponse missions =
				experimentProgressService.getMissions(member.getId(), started.userExperimentProgramId());

		assertTrue(missions.days().get(1).replaced());
		assertFalse(missions.days().get(0).replaced());
	}

	@Test
	void pause_inProgressCourse_marksPausedWithoutTouchingProgress() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());

		var response = experimentProgressService.pause(member.getId(), started.userExperimentProgramId());

		assertEquals(UserExperimentProgramStatus.PAUSED, response.status());
		var paused = userExperimentProgramRepository.findById(started.userExperimentProgramId()).orElseThrow();
		assertEquals(UserExperimentProgramStatus.PAUSED, paused.getStatus());
		assertEquals((short) 1, paused.getCurrentDay());
	}

	@Test
	void pause_alreadyPaused_throwsUserProgramNotInProgress() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		experimentProgressService.pause(member.getId(), started.userExperimentProgramId());

		BusinessException exception = assertThrows(BusinessException.class,
				() -> experimentProgressService.pause(member.getId(), started.userExperimentProgramId()));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_NOT_IN_PROGRESS, exception.errorCode());
	}

	@Test
	void recordMission_pausedCourse_autoResumesToInProgress() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgramStartResponse started = startThreeDayCourse(member.getId());
		experimentProgressService.pause(member.getId(), started.userExperimentProgramId());

		ExperimentMissionRecordResponse response = experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				recordRequest(ExperimentAttemptStatus.DONE, LocalDate.now(), false));

		assertEquals(UserExperimentProgramStatus.IN_PROGRESS, response.status());
		assertEquals(
				UserExperimentProgramStatus.IN_PROGRESS,
				userExperimentProgramRepository.findById(started.userExperimentProgramId()).orElseThrow().getStatus());
	}

	private void recordDay(UUID memberId, UUID userExperimentProgramId, int dayNumber, ExperimentAttemptStatus attemptStatus) {
		UserProgramMission slot = userProgramMissionRepository
				.findByUserExperimentProgram_IdAndDayNumber(userExperimentProgramId, (short) dayNumber)
				.orElseThrow();
		experimentProgressService.recordMission(
				memberId, userExperimentProgramId, slot.getId(), recordRequest(attemptStatus, LocalDate.now(), false));
	}

	private ExperimentMissionRecordRequest recordRequest(
			ExperimentAttemptStatus attemptStatus, LocalDate recordDate, boolean createLifeTimeEntry) {
		return new ExperimentMissionRecordRequest(
				attemptStatus, recordDate, "오늘의 기록", null, List.of(), null, null, createLifeTimeEntry);
	}

	private ExperimentProgramStartResponse startThreeDayCourse(UUID memberId) {
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		return experimentEnrollmentService.startFromTemplate(
				memberId, program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));
	}

}
