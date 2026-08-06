package com.naroom.api.experiment;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.dto.ExperimentMissionCatalogResponse;
import com.naroom.api.experiment.dto.ExperimentMissionRecordRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartRequest;
import com.naroom.api.experiment.dto.ExperimentProgramStartResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// V16 시드 데이터를 활용해 미션 카탈로그 조회(E06/E08/E11 교체 후보 목록)를 검증한다.
@SpringBootTest
@Transactional
class ExperimentMissionCatalogServiceTest {

	@Autowired
	private ExperimentMissionCatalogService experimentMissionCatalogService;

	@Autowired
	private ExperimentEnrollmentService experimentEnrollmentService;

	@Autowired
	private ExperimentProgressService experimentProgressService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ExperimentProgramRepository experimentProgramRepository;

	@Test
	void list_noFilter_returnsActiveMissionsOnly() {
		Member member = memberRepository.save(Member.create("지연"));

		List<ExperimentMissionCatalogResponse> missions = experimentMissionCatalogService.list(member.getId(), null);

		assertFalse(missions.isEmpty());
	}

	@Test
	void list_withTopicCode_returnsOnlyThatTopic() {
		Member member = memberRepository.save(Member.create("지연"));

		List<ExperimentMissionCatalogResponse> missions = experimentMissionCatalogService.list(member.getId(), "EMOTION");

		assertFalse(missions.isEmpty());
		assertTrue(missions.stream().allMatch(mission -> mission.topicCode().equals("EMOTION")));
	}

	@Test
	void list_afterNotAFitRecord_excludesThatMission() {
		Member member = memberRepository.save(Member.create("지연"));
		ExperimentProgram program = experimentProgramRepository.findByCode("NOW_MIND_3").orElseThrow();
		ExperimentProgramStartResponse started = experimentEnrollmentService.startFromTemplate(
				member.getId(), program.getId(), new ExperimentProgramStartRequest(null, List.of(), null, false));
		UUID notAFitMissionId = started.todayMission().missionId();

		experimentProgressService.recordMission(
				member.getId(), started.userExperimentProgramId(), started.todayMission().userProgramMissionId(),
				new ExperimentMissionRecordRequest(
						ExperimentAttemptStatus.NOT_A_FIT, LocalDate.now(), null, null, List.of(), null, null, false));

		List<ExperimentMissionCatalogResponse> missions = experimentMissionCatalogService.list(member.getId(), null);

		assertTrue(missions.stream().noneMatch(mission -> mission.id().equals(notAFitMissionId)));
	}

}
