package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 회원 수행 이력(NOT_A_FIT 제외/빈도 기반 우선순위 낮춤)은 8-D에서 실제 기록 API가 생기면 그 위에서
// 검증한다 - 여기서는 이력이 없는 새 회원 기준으로 §7.4의 구조적 규칙(중복 금지·마지막 날 REVIEW)만 확인한다.
@SpringBootTest
@Transactional
class ExperimentRandomProgramComposerTest {

	@Autowired
	private ExperimentRandomProgramComposer experimentRandomProgramComposer;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void compose_threeDay_returnsThreeDistinctMissionsEndingInReview() {
		Member member = memberRepository.save(Member.create("지연"));

		ExperimentRandomProgramResponse response = experimentRandomProgramComposer.compose(member.getId(), (short) 3);

		assertEquals(3, response.durationDays());
		assertDistinctMissionsInDayOrder(response.missions());
		assertEquals(
				ExperimentMissionType.REVIEW,
				response.missions().get(response.missions().size() - 1).missionType());
	}

	@Test
	void compose_sevenDay_returnsSevenDistinctMissionsEndingInReview() {
		Member member = memberRepository.save(Member.create("지연"));

		ExperimentRandomProgramResponse response = experimentRandomProgramComposer.compose(member.getId(), (short) 7);

		assertEquals(7, response.durationDays());
		assertDistinctMissionsInDayOrder(response.missions());
		assertEquals(
				ExperimentMissionType.REVIEW,
				response.missions().get(response.missions().size() - 1).missionType());
	}

	@Test
	void compose_invalidDuration_throwsBusinessException() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> experimentRandomProgramComposer.compose(member.getId(), (short) 5));

		assertEquals(ExperimentErrorCode.DURATION_INVALID, exception.errorCode());
	}

	private static void assertDistinctMissionsInDayOrder(List<ExperimentProgramMissionResponse> missions) {
		Set<UUID> missionIds = missions.stream()
				.map(ExperimentProgramMissionResponse::missionId)
				.collect(Collectors.toCollection(HashSet::new));
		assertEquals(missions.size(), missionIds.size());
		for (int i = 0; i < missions.size(); i++) {
			assertEquals(i + 1, missions.get(i).dayNumber());
		}
	}

}
