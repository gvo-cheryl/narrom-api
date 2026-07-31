package com.naroom.api.experiment.domain.repository;

import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserExperimentProgramRepository extends JpaRepository<UserExperimentProgram, UUID> {

	// DEC-02: 활성 상태(IN_PROGRESS/PAUSED/AWAITING_REVIEW)는 회원당 1개다 - DB 부분 유니크
	// 인덱스(uq_user_experiment_one_active)와 함께 서비스 계층에서도 동일한 상태 집합으로 조회한다.
	Optional<UserExperimentProgram> findByMember_IdAndStatusIn(UUID memberId, Collection<UserExperimentProgramStatus> statuses);

	Optional<UserExperimentProgram> findByIdAndMember_Id(UUID id, UUID memberId);

	List<UserExperimentProgram> findByMember_IdOrderByCreatedAtDesc(UUID memberId);

}
