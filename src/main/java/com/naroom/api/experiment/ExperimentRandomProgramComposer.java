package com.naroom.api.experiment;

import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentEmotionalLoad;
import com.naroom.api.experiment.domain.entity.ExperimentMission;
import com.naroom.api.experiment.domain.entity.ExperimentMissionType;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository.MissionAttemptCount;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRepository;
import com.naroom.api.experiment.dto.ExperimentProgramMissionResponse;
import com.naroom.api.experiment.dto.ExperimentRandomProgramResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// §7.4 자동·랜덤 구성 규칙을 그대로 구현한다. 아무것도 저장하지 않는 미리보기이고(§16 랜덤 시작
// 계약은 8-C), 회원마다 매번 다른 후보를 돌려줄 수 있다.
@Service
@Transactional(readOnly = true)
public class ExperimentRandomProgramComposer {

	private static final Set<Short> ALLOWED_DURATIONS = Set.of((short) 3, (short) 7);
	private static final Set<ExperimentMissionType> FIRST_DAY_TYPES =
			Set.of(ExperimentMissionType.OBSERVATION, ExperimentMissionType.RECORD);

	private final ExperimentMissionRepository experimentMissionRepository;
	private final ExperimentMissionRecordRepository experimentMissionRecordRepository;

	public ExperimentRandomProgramComposer(
			ExperimentMissionRepository experimentMissionRepository,
			ExperimentMissionRecordRepository experimentMissionRecordRepository) {
		this.experimentMissionRepository = experimentMissionRepository;
		this.experimentMissionRecordRepository = experimentMissionRecordRepository;
	}

	public ExperimentRandomProgramResponse compose(UUID memberId, short durationDays) {
		if (!ALLOWED_DURATIONS.contains(durationDays)) {
			throw new BusinessException(ExperimentErrorCode.DURATION_INVALID);
		}

		Set<UUID> notAFitMissionIds = experimentMissionRecordRepository.findMissionIdsByMemberAndAttemptStatus(
				memberId, ExperimentAttemptStatus.NOT_A_FIT);
		Map<UUID, Long> attemptCounts = experimentMissionRecordRepository.countAttemptsByMissionForMember(memberId).stream()
				.collect(Collectors.toMap(MissionAttemptCount::getMissionId, MissionAttemptCount::getAttemptCount));

		List<ExperimentMission> pool = experimentMissionRepository.findByActiveTrue().stream()
				.filter(mission -> !notAFitMissionIds.contains(mission.getId()))
				// 이미 여러 번 진행한 미션은 우선순위를 낮춘다 - 시도 횟수 오름차순으로 정렬한 뒤
				// 같은 순위 안에서는 무작위로 섞어 매번 다른 후보가 나오게 한다.
				.sorted(Comparator.comparingLong(mission -> attemptCounts.getOrDefault(mission.getId(), 0L)))
				.collect(Collectors.toCollection(ArrayList::new));
		shuffleWithinEqualAttemptCounts(pool, attemptCounts);

		List<ExperimentMission> picked = new ArrayList<>();
		Set<UUID> usedIds = new HashSet<>();

		ExperimentMission firstDay = pickFirst(
				pool, usedIds, mission -> FIRST_DAY_TYPES.contains(mission.getMissionType())
						&& mission.getEmotionalLoad() == ExperimentEmotionalLoad.LOW)
				.or(() -> pickFirst(pool, usedIds, mission -> FIRST_DAY_TYPES.contains(mission.getMissionType())))
				.or(() -> pickFirst(pool, usedIds, mission -> true))
				.orElseThrow();
		picked.add(firstDay);
		usedIds.add(firstDay.getId());

		ExperimentMissionType lastType = firstDay.getMissionType();
		ExperimentEmotionalLoad lastLoad = firstDay.getEmotionalLoad();
		for (int day = 2; day < durationDays; day++) {
			ExperimentMissionType excludedType = lastType;
			boolean avoidHighLoad = lastLoad == ExperimentEmotionalLoad.HIGH;
			ExperimentMission next = pickFirst(
					pool, usedIds, mission -> mission.getMissionType() != ExperimentMissionType.REVIEW
							&& mission.getMissionType() != excludedType
							&& (!avoidHighLoad || mission.getEmotionalLoad() != ExperimentEmotionalLoad.HIGH))
					.or(() -> pickFirst(pool, usedIds, mission -> mission.getMissionType() != ExperimentMissionType.REVIEW))
					.or(() -> pickFirst(pool, usedIds, mission -> true))
					.orElseThrow();
			picked.add(next);
			usedIds.add(next.getId());
			lastType = next.getMissionType();
			lastLoad = next.getEmotionalLoad();
		}

		ExperimentMission lastDay = pickFirst(pool, usedIds, mission -> mission.getMissionType() == ExperimentMissionType.REVIEW)
				.or(() -> pickFirst(pool, usedIds, mission -> true))
				.orElseThrow();
		picked.add(lastDay);

		List<ExperimentProgramMissionResponse> missions = new ArrayList<>();
		for (int i = 0; i < picked.size(); i++) {
			missions.add(ExperimentProgramMissionResponse.of((short) (i + 1), picked.get(i)));
		}
		return new ExperimentRandomProgramResponse(durationDays, missions);
	}

	private static Optional<ExperimentMission> pickFirst(
			List<ExperimentMission> pool, Set<UUID> usedIds, Predicate<ExperimentMission> filter) {
		return pool.stream()
				.filter(mission -> !usedIds.contains(mission.getId()))
				.filter(filter)
				.findFirst();
	}

	private static void shuffleWithinEqualAttemptCounts(List<ExperimentMission> pool, Map<UUID, Long> attemptCounts) {
		int start = 0;
		while (start < pool.size()) {
			long count = attemptCounts.getOrDefault(pool.get(start).getId(), 0L);
			int end = start + 1;
			while (end < pool.size() && attemptCounts.getOrDefault(pool.get(end).getId(), 0L) == count) {
				end++;
			}
			shuffleRange(pool, start, end);
			start = end;
		}
	}

	private static void shuffleRange(List<ExperimentMission> pool, int fromInclusive, int toExclusive) {
		for (int i = toExclusive - 1; i > fromInclusive; i--) {
			int j = fromInclusive + ThreadLocalRandom.current().nextInt(i - fromInclusive + 1);
			ExperimentMission temp = pool.get(i);
			pool.set(i, pool.get(j));
			pool.set(j, temp);
		}
	}

}
