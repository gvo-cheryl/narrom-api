package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.UserProgramMissionRepository;
import com.naroom.api.experiment.dto.ExperimentCourseReviewRequest;
import com.naroom.api.experiment.dto.ExperimentCourseReviewResponse;
import com.naroom.api.experiment.dto.ExperimentEndEarlyResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordRequest;
import com.naroom.api.experiment.dto.ExperimentPastProgramResponse;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드 데이터를 활용해 §5.4 코스 종료(돌아보기 저장/지금까지 기록하고 마무리하기)와 §11.3 코스 전용
// AI 회고 통합을 검증한다.
@SpringBootTest
@Transactional
class ExperimentReviewServiceTest {

	@Autowired
	private ExperimentReviewService experimentReviewService;

	@Autowired
	private ExperimentEnrollmentService experimentEnrollmentService;

	@Autowired
	private ExperimentProgressService experimentProgressService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ExperimentProgramRepository experimentProgramRepository;

	@Autowired
	private UserExperimentProgramRepository userExperimentProgramRepository;

	@Autowired
	private UserProgramMissionRepository userProgramMissionRepository;

	@Test
	void completeReview_awaitingReviewCourse_marksCompletedAndCreatesLifeTimeEntry() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID userExperimentProgramId = startAndFinishAllDays(member.getId(), "NOW_MIND_3", 3);

		ExperimentCourseReviewResponse response = experimentReviewService.completeReview(
				member.getId(), userExperimentProgramId, reviewRequest(false));

		assertEquals(UserExperimentProgramStatus.COMPLETED, response.status());
		assertTrue(response.lifeTimeEntryCreated());
		assertNull(response.aiJob());
		assertEquals(
				UserExperimentProgramStatus.COMPLETED,
				userExperimentProgramRepository.findById(userExperimentProgramId).orElseThrow().getStatus());
	}

	@Test
	void completeReview_notAwaitingReview_throwsUserProgramNotAwaitingReview() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		ExperimentProgramStartResponse started = experimentEnrollmentService.startFromTemplate(
				member.getId(), program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentReviewService.completeReview(
				member.getId(), started.userExperimentProgramId(), reviewRequest(false)));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_NOT_AWAITING_REVIEW, exception.errorCode());
	}

	@Test
	void completeReview_threeDayCourseWithAiReflection_createsAiJob() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID userExperimentProgramId = startAndFinishAllDays(member.getId(), "NOW_MIND_3", 3);

		ExperimentCourseReviewResponse response = experimentReviewService.completeReview(
				member.getId(), userExperimentProgramId, reviewRequest(true));

		assertNotNull(response.aiJob());
		assertEquals(AiFeatureType.THREE_DAY_REFLECTION, response.aiJob().featureType());
		assertNotNull(response.aiJob().status());
	}

	@Test
	void completeReview_sevenDayCourseWithAiReflection_returnsNoteWithoutAiJob() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID userExperimentProgramId = startAndFinishAllDays(member.getId(), "FIND_START_WAY_7", 7);

		ExperimentCourseReviewResponse response = experimentReviewService.completeReview(
				member.getId(), userExperimentProgramId, reviewRequest(true));

		assertNotNull(response.aiJob());
		assertNull(response.aiJob().featureType());
		assertNotNull(response.aiJob().note());
	}

	@Test
	void endEarly_inProgressCourse_marksEndedEarly() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		ExperimentProgramStartResponse started = experimentEnrollmentService.startFromTemplate(
				member.getId(), program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));

		ExperimentEndEarlyResponse response = experimentReviewService.endEarly(member.getId(), started.userExperimentProgramId());

		assertEquals(UserExperimentProgramStatus.ENDED_EARLY, response.status());
	}

	@Test
	void endEarly_alreadyEnded_throwsUserProgramAlreadyEnded() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		ExperimentProgramStartResponse started = experimentEnrollmentService.startFromTemplate(
				member.getId(), program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));
		experimentReviewService.endEarly(member.getId(), started.userExperimentProgramId());

		BusinessException exception = assertThrows(BusinessException.class, () -> experimentReviewService.endEarly(
				member.getId(), started.userExperimentProgramId()));

		assertEquals(ExperimentErrorCode.USER_PROGRAM_ALREADY_ENDED, exception.errorCode());
	}

	@Test
	void listPast_returnsOnlyCompletedAndEndedEarly() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID completedId = startAndFinishAllDays(member.getId(), "NOW_MIND_3", 3);
		experimentReviewService.completeReview(member.getId(), completedId, reviewRequest(false));

		ExperimentProgram sevenDay = experimentProgramRepository.findByCode("FIND_START_WAY_7").orElseThrow();
		ExperimentProgramStartResponse endedEarlyStarted = experimentEnrollmentService.startFromTemplate(
				member.getId(), sevenDay.getId(), new ExperimentProgramStartRequest(null, List.of(), null, true));
		experimentReviewService.endEarly(member.getId(), endedEarlyStarted.userExperimentProgramId());

		List<ExperimentPastProgramResponse> past = experimentReviewService.listPast(member.getId());

		assertEquals(2, past.size());
		assertTrue(past.stream().allMatch(p ->
				p.status() == UserExperimentProgramStatus.COMPLETED || p.status() == UserExperimentProgramStatus.ENDED_EARLY));
	}

	private UUID startAndFinishAllDays(UUID memberId, String programCode, int durationDays) {
		ExperimentProgram program = experimentProgramRepository.findByCode(programCode).orElseThrow();
		ExperimentProgramStartResponse started = experimentEnrollmentService.startFromTemplate(
				memberId, program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, true));
		for (int day = 1; day <= durationDays; day++) {
			var slot = userProgramMissionRepository
					.findByUserExperimentProgram_IdAndDayNumber(started.userExperimentProgramId(), (short) day)
					.orElseThrow();
			experimentProgressService.recordMission(
					memberId, started.userExperimentProgramId(), slot.getId(),
					new ExperimentMissionRecordRequest(
							ExperimentAttemptStatus.DONE, LocalDate.now(), "오늘의 기록", null, List.of(), null, null, true));
		}
		return started.userExperimentProgramId();
	}

	private ExperimentCourseReviewRequest reviewRequest(boolean requestAiReflection) {
		return new ExperimentCourseReviewRequest(
				(short) 1, (short) 2, null, List.of("조건1"), List.of(), "발견한 것", "계속할 행동", "나에 대한 요약",
				requestAiReflection);
	}

}
