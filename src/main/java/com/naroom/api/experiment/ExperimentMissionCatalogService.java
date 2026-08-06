package com.naroom.api.experiment;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.dto.ExperimentMissionCatalogResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 시작 전 미션 교체(E06)·진행 중 미션 교체(E08/E11)에서 "바꿀 미션"을 고를 때 쓰는 대체 후보
// 목록이다. 지금까지는 이 목록을 조회할 방법이 없어 프론트가 교체 화면을 만들 수 없었다.
@Service
@Transactional(readOnly = true)
public class ExperimentMissionCatalogService {

	private final ExperimentMissionRepository experimentMissionRepository;
	private final ExperimentMissionRecordRepository experimentMissionRecordRepository;

	public ExperimentMissionCatalogService(
			ExperimentMissionRepository experimentMissionRepository,
			ExperimentMissionRecordRepository experimentMissionRecordRepository) {
		this.experimentMissionRepository = experimentMissionRepository;
		this.experimentMissionRecordRepository = experimentMissionRecordRepository;
	}

	// §7.4와 동일하게 이 회원이 이전에 NOT_A_FIT으로 기록한 미션은 후보에서 제외한다
	// (ExperimentRandomProgramComposer와 같은 규칙).
	public List<ExperimentMissionCatalogResponse> list(UUID memberId, String topicCode) {
		Set<UUID> notAFitMissionIds = experimentMissionRecordRepository
				.findMissionIdsByMemberAndAttemptStatus(memberId, ExperimentAttemptStatus.NOT_A_FIT);
		return experimentMissionRepository.findByActiveTrue().stream()
				.filter(mission -> !notAFitMissionIds.contains(mission.getId()))
				.filter(mission -> topicCode == null || mission.getTopic().getCode().equals(topicCode))
				.sorted(Comparator.comparing(ExperimentMission::getTitle))
				.map(ExperimentMissionCatalogResponse::from)
				.toList();
	}

}
