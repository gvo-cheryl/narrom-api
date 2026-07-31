package com.naroom.api.experiment;

import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.dto.ExperimentProgramDetailResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSummaryResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드(13개 주제, 58개 미션, 13개 코스)가 이미 적용돼 있다는 전제로, 시드 데이터를 그대로 검증한다.
@SpringBootTest
@Transactional
class ExperimentProgramServiceTest {

	@Autowired
	private ExperimentProgramService experimentProgramService;

	@Test
	void list_filtersByDurationDays_returnsOnlyThreeDayPrograms() {
		List<ExperimentProgramSummaryResponse> programs =
				experimentProgramService.list((short) 3, null, null, null);

		assertEquals(6, programs.size());
		assertTrue(programs.stream().allMatch(p -> p.durationDays() == 3));
	}

	@Test
	void list_filtersByTopicCode_returnsOnlyMatchingTopic() {
		List<ExperimentProgramSummaryResponse> programs =
				experimentProgramService.list(null, "EMOTION", null, null);

		assertTrue(programs.stream().allMatch(p -> p.topicCode().equals("EMOTION")));
		assertTrue(programs.stream().anyMatch(p -> p.code().equals("NOW_MIND_3")));
	}

	@Test
	void list_filtersByFeatured_returnsOnlyFeaturedPrograms() {
		List<ExperimentProgramSummaryResponse> programs = experimentProgramService.list(null, null, true, null);

		assertTrue(programs.size() >= 8);
	}

	@Test
	void list_invalidDurationDays_throwsBusinessException() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentProgramService.list((short) 5, null, null, null));

		assertEquals(ExperimentErrorCode.DURATION_INVALID, exception.errorCode());
	}

	@Test
	void getDetail_returnsMissionsOrderedByDay() {
		ExperimentProgramSummaryResponse nowMind3 = experimentProgramService.list((short) 3, "EMOTION", null, null)
				.stream()
				.filter(p -> p.code().equals("NOW_MIND_3"))
				.findFirst()
				.orElseThrow();

		ExperimentProgramDetailResponse detail = experimentProgramService.getDetail(nowMind3.programId());

		assertEquals("NOW_MIND_3", detail.code());
		assertEquals(3, detail.missions().size());
		assertEquals(1, detail.missions().get(0).dayNumber());
		assertEquals(2, detail.missions().get(1).dayNumber());
		assertEquals(3, detail.missions().get(2).dayNumber());
	}

	@Test
	void getDetail_unknownId_throwsProgramNotFound() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentProgramService.getDetail(UUID.randomUUID()));

		assertEquals(ExperimentErrorCode.PROGRAM_NOT_FOUND, exception.errorCode());
	}

}
