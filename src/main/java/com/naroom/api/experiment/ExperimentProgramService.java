package com.naroom.api.experiment;

import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentProgramMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.dto.ExperimentProgramDetailResponse;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentProgramSummaryResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// E01(홈)/E02(추천)/E03(주제별) 목록과 E04(코스 상세) 조회를 담당한다. 코스 원본은 13개뿐이라
// 필터별로 별도 쿼리를 만들지 않고 PUBLISHED 전체를 가져와 Java에서 거른다(TagExplorationService와
// 동일한 방식).
@Service
@Transactional(readOnly = true)
public class ExperimentProgramService {

	private final ExperimentProgramRepository experimentProgramRepository;
	private final ExperimentProgramMissionRepository experimentProgramMissionRepository;

	public ExperimentProgramService(
			ExperimentProgramRepository experimentProgramRepository,
			ExperimentProgramMissionRepository experimentProgramMissionRepository) {
		this.experimentProgramRepository = experimentProgramRepository;
		this.experimentProgramMissionRepository = experimentProgramMissionRepository;
	}

	public List<ExperimentProgramSummaryResponse> list(Short durationDays, String topicCode, Boolean featured, Boolean beginner) {
		if (durationDays != null && durationDays != 3 && durationDays != 7) {
			throw new BusinessException(ExperimentErrorCode.DURATION_INVALID);
		}
		return experimentProgramRepository.findByStatusOrderByDisplayOrderAsc(ExperimentProgramStatus.PUBLISHED).stream()
				.filter(program -> durationDays == null || program.getDurationDays() == durationDays)
				.filter(program -> topicCode == null || program.getPrimaryTopic().getCode().equals(topicCode))
				.filter(program -> featured == null || program.isFeatured() == featured)
				.filter(program -> beginner == null || program.isBeginner() == beginner)
				.map(ExperimentProgramSummaryResponse::of)
				.toList();
	}

	public ExperimentProgramDetailResponse getDetail(UUID programId) {
		ExperimentProgram program = experimentProgramRepository.findById(programId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.PROGRAM_NOT_FOUND));
		List<ExperimentProgramMissionResponse> missions =
				experimentProgramMissionRepository.findByProgram_IdOrderById_DayNumberAsc(programId).stream()
						.map(ExperimentProgramMissionResponse::from)
						.toList();
		return ExperimentProgramDetailResponse.of(program, missions);
	}

}
