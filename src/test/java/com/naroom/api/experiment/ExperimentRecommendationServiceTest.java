package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.CheckInService;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationSourceType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentRecommendationResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드 데이터를 활용해 §14 추천 로직(Beta 1에서 확정 가능한 신호만) 중 처음 시작·체크인 에너지
// 낮음 반복 규칙과 살펴봄/넘김 상태 전이를 검증한다.
@SpringBootTest
@Transactional
class ExperimentRecommendationServiceTest {

	@Autowired
	private ExperimentRecommendationService experimentRecommendationService;

	@Autowired
	private ExperimentEnrollmentService experimentEnrollmentService;

	@Autowired
	private CheckInService checkInService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ExperimentProgramRepository experimentProgramRepository;

	@Test
	void listActive_memberNeverStarted_recommendsFirstTimePrograms() {
		Member member = memberRepository.save(Member.create("지연"));

		List<ExperimentRecommendationResponse> recommendations = experimentRecommendationService.listActive(member.getId());

		Set<String> programCodes = recommendations.stream()
				.map(recommendation -> recommendation.program().code())
				.collect(Collectors.toSet());
		assertTrue(programCodes.contains("NOW_MIND_3"));
		assertTrue(programCodes.contains("KNOW_ME_LIGHTLY_7"));
		assertTrue(recommendations.stream().allMatch(r -> r.sourceType() == ExperimentRecommendationSourceType.RULE));
	}

	@Test
	void listActive_calledTwice_doesNotCreateDuplicateRecommendations() {
		Member member = memberRepository.save(Member.create("지연"));

		int firstCallCount = experimentRecommendationService.listActive(member.getId()).size();
		int secondCallCount = experimentRecommendationService.listActive(member.getId()).size();

		assertEquals(firstCallCount, secondCallCount);
	}

	@Test
	void listActive_lowEnergyRepeatedCheckIns_recommendsRecoveryClues() {
		Member member = memberRepository.save(Member.create("지연"));
		startAnyProgram(member.getId());
		upsertCheckIn(member.getId(), LocalDate.now().minusDays(2), (short) 20);
		upsertCheckIn(member.getId(), LocalDate.now().minusDays(1), (short) 30);

		List<ExperimentRecommendationResponse> recommendations = experimentRecommendationService.listActive(member.getId());

		assertTrue(recommendations.stream().anyMatch(r -> r.program().code().equals("RECOVERY_CLUES_3")
				&& r.sourceType() == ExperimentRecommendationSourceType.CHECK_IN));
	}

	@Test
	void listActive_singleLowEnergyCheckIn_doesNotRecommendRecoveryClues() {
		Member member = memberRepository.save(Member.create("지연"));
		startAnyProgram(member.getId());
		upsertCheckIn(member.getId(), LocalDate.now(), (short) 20);

		List<ExperimentRecommendationResponse> recommendations = experimentRecommendationService.listActive(member.getId());

		assertFalse(recommendations.stream().anyMatch(r -> r.program().code().equals("RECOVERY_CLUES_3")));
	}

	@Test
	void markViewed_shownRecommendation_transitionsToViewed() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID recommendationId = experimentRecommendationService.listActive(member.getId()).get(0).recommendationId();

		ExperimentRecommendationResponse response = experimentRecommendationService.markViewed(member.getId(), recommendationId);

		assertEquals(ExperimentRecommendationStatus.VIEWED, response.status());
	}

	@Test
	void dismiss_viewedRecommendation_transitionsToDismissed() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID recommendationId = experimentRecommendationService.listActive(member.getId()).get(0).recommendationId();
		experimentRecommendationService.markViewed(member.getId(), recommendationId);

		ExperimentRecommendationResponse response = experimentRecommendationService.dismiss(member.getId(), recommendationId);

		assertEquals(ExperimentRecommendationStatus.DISMISSED, response.status());
	}

	@Test
	void dismiss_alreadyDismissed_throwsRecommendationNotActionable() {
		Member member = memberRepository.save(Member.create("지연"));
		UUID recommendationId = experimentRecommendationService.listActive(member.getId()).get(0).recommendationId();
		experimentRecommendationService.dismiss(member.getId(), recommendationId);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> experimentRecommendationService.dismiss(member.getId(), recommendationId));

		assertEquals(ExperimentErrorCode.RECOMMENDATION_NOT_ACTIONABLE, exception.errorCode());
	}

	private void startAnyProgram(UUID memberId) {
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		experimentEnrollmentService.startFromTemplate(
				memberId, program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));
	}

	private void upsertCheckIn(UUID memberId, LocalDate checkInDate, short energyLevel) {
		checkInService.upsertCheckIn(memberId, new CheckInUpsertRequest(
				checkInDate, null, energyLevel, null, null, null, null, List.of()));
	}

}
