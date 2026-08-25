package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentProgramRepository extends JpaRepository<ExperimentProgram, UUID>, JpaSpecificationExecutor<ExperimentProgram> {

	List<ExperimentProgram> findByStatusOrderByDisplayOrderAsc(ExperimentProgramStatus status);

	List<ExperimentProgram> findByStatusAndDurationDaysOrderByDisplayOrderAsc(
			ExperimentProgramStatus status, short durationDays);

	List<ExperimentProgram> findByStatusAndPrimaryTopic_IdOrderByDisplayOrderAsc(
			ExperimentProgramStatus status, UUID primaryTopicId);

	Optional<ExperimentProgram> findByCode(String code);

	// 관리자 버전 관리(V26) 도입 후 같은 code가 여러 row(DRAFT/PUBLISHED/ARCHIVED)에 걸쳐 있을 수 있다 -
	// "현재 이 code로 노출 중인 프로그램"을 찾을 때는 findByCode 대신 이 메서드를 쓴다.
	Optional<ExperimentProgram> findByCodeAndStatus(String code, ExperimentProgramStatus status);

	boolean existsByCode(String code);

}
