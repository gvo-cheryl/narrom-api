package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMissionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExperimentMissionRecordRepository extends JpaRepository<ExperimentMissionRecord, UUID> {

	List<ExperimentMissionRecord> findByUserExperimentProgram_IdOrderByRecordDateDesc(UUID userExperimentProgramId);

	Optional<ExperimentMissionRecord> findByUserProgramMission_Id(UUID userProgramMissionId);

	// DEC-03: 같은 날짜에 RESTED는 한 번만 기록한다(uq_experiment_one_rest_per_date와 짝을 이룬다).
	Optional<ExperimentMissionRecord> findByUserExperimentProgram_IdAndRecordDateAndAttemptStatus(
			UUID userExperimentProgramId, LocalDate recordDate, ExperimentAttemptStatus attemptStatus);

	// §7.4 제한적 랜덤 코스 규칙: 이전에 NOT_A_FIT으로 기록한 미션은 재추천을 제한한다.
	// attemptStatus는 바인드 파라미터로 넘긴다 - JPQL에 enum 리터럴을 직접 쓰면 Hibernate가 Java
	// 단순 클래스명(ExperimentAttemptStatus)으로 네이티브 캐스트를 만들어, 실제 Postgres 타입명
	// (experiment_attempt_status)과 달라 SQLGrammarException이 난다.
	@Query("""
			select distinct emr.userProgramMission.mission.id
			from ExperimentMissionRecord emr
			where emr.userExperimentProgram.member.id = :memberId
			  and emr.attemptStatus = :attemptStatus
			  and emr.userProgramMission.mission.id is not null
			""")
	Set<UUID> findMissionIdsByMemberAndAttemptStatus(
			@Param("memberId") UUID memberId, @Param("attemptStatus") ExperimentAttemptStatus attemptStatus);

	// §7.4: 사용자가 이미 여러 번 진행한 미션은 우선순위를 낮춘다 - 미션별 시도 횟수를 센다.
	@Query("""
			select emr.userProgramMission.mission.id as missionId, count(emr) as attemptCount
			from ExperimentMissionRecord emr
			where emr.userExperimentProgram.member.id = :memberId
			  and emr.userProgramMission.mission.id is not null
			group by emr.userProgramMission.mission.id
			""")
	List<MissionAttemptCount> countAttemptsByMissionForMember(@Param("memberId") UUID memberId);

	interface MissionAttemptCount {
		UUID getMissionId();

		long getAttemptCount();
	}

}
