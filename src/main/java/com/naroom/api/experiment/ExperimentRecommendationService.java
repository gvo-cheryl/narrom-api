package com.naroom.api.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.experiment.domain.entity.ExperimentAttemptStatus;
import com.naroom.api.experiment.domain.entity.ExperimentProgram;
import com.naroom.api.experiment.domain.entity.ExperimentProgramMission;
import com.naroom.api.experiment.domain.entity.ExperimentProgramStatus;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendation;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationSourceType;
import com.naroom.api.experiment.domain.entity.ExperimentRecommendationStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramMissionRepository;
import com.naroom.api.experiment.domain.repository.ExperimentProgramRepository;
import com.naroom.api.experiment.domain.repository.ExperimentRecommendationRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.dto.ExperimentRecommendationResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// §14 추천 로직 Beta 1. 확정 가능한 신호(처음 시작, 체크인 에너지 낮음 반복)만 규칙 기반으로 구현하고,
// 태그 등으로 구조화되지 않은 "시작 어려움/관계·대화/늦은 수면" 신호는 근거 없는 추측이 되므로 Beta 1
// 범위에서 제외한다(문서화된 결정, 놓친 게 아님). 체크인·기록 도메인에는 신호 조회용 읽기 메서드만
// 추가하고, 추천 계산은 이 서비스가 GET 요청을 받을 때마다 온디맨드로 평가한다 - 체크인/기록 생성
// 흐름에 훅을 걸지 않아 두 도메인을 건드리지 않는다.
@Service
@Transactional(readOnly = true)
public class ExperimentRecommendationService {

	private static final short LOW_ENERGY_THRESHOLD = 40;
	private static final int LOW_ENERGY_MIN_OCCURRENCES = 2;
	private static final Duration RECOMMENDATION_FRESHNESS = Duration.ofDays(7);
	private static final Set<ExperimentRecommendationStatus> ACTIVE_STATUSES =
			Set.of(ExperimentRecommendationStatus.SHOWN, ExperimentRecommendationStatus.VIEWED);
	private static final Set<ExperimentRecommendationStatus> RESPONDED_STATUSES = Set.of(
			ExperimentRecommendationStatus.DISMISSED, ExperimentRecommendationStatus.ACCEPTED,
			ExperimentRecommendationStatus.EXPIRED);

	private static final String REASON_FIRST_TIME = "FIRST_TIME";
	private static final String REASON_LOW_ENERGY_REPEATED = "LOW_ENERGY_REPEATED";
	private static final String FIRST_TIME_REASON_TEXT = "작은 실험이 처음이라면 이 코스로 가볍게 시작해볼 수 있어요.";

	// 요청 record 자유 JSON 저장과 같은 이유(ObjectMapper가 스프링 빈으로 없음) - LocalDate 대신
	// 문자열로 직렬화해 JavaTimeModule 등록 없이도 안전하게 쓴다.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final MemberRepository memberRepository;
	private final ExperimentProgramRepository experimentProgramRepository;
	private final ExperimentProgramMissionRepository experimentProgramMissionRepository;
	private final ExperimentRecommendationRepository experimentRecommendationRepository;
	private final ExperimentMissionRecordRepository experimentMissionRecordRepository;
	private final UserExperimentProgramRepository userExperimentProgramRepository;
	private final CheckInRepository checkInRepository;

	public ExperimentRecommendationService(
			MemberRepository memberRepository,
			ExperimentProgramRepository experimentProgramRepository,
			ExperimentProgramMissionRepository experimentProgramMissionRepository,
			ExperimentRecommendationRepository experimentRecommendationRepository,
			ExperimentMissionRecordRepository experimentMissionRecordRepository,
			UserExperimentProgramRepository userExperimentProgramRepository,
			CheckInRepository checkInRepository) {
		this.memberRepository = memberRepository;
		this.experimentProgramRepository = experimentProgramRepository;
		this.experimentProgramMissionRepository = experimentProgramMissionRepository;
		this.experimentRecommendationRepository = experimentRecommendationRepository;
		this.experimentMissionRecordRepository = experimentMissionRecordRepository;
		this.userExperimentProgramRepository = userExperimentProgramRepository;
		this.checkInRepository = checkInRepository;
	}

	@Transactional
	public List<ExperimentRecommendationResponse> listActive(UUID memberId) {
		generateRuleBasedRecommendations(memberId);

		Instant now = Instant.now();
		return experimentRecommendationRepository
				.findByMember_IdAndStatusInOrderByCreatedAtDesc(memberId, ACTIVE_STATUSES).stream()
				.filter(recommendation -> recommendation.getExpiresAt() == null || recommendation.getExpiresAt().isAfter(now))
				.map(ExperimentRecommendationResponse::of)
				.toList();
	}

	@Transactional
	public ExperimentRecommendationResponse markViewed(UUID memberId, UUID recommendationId) {
		ExperimentRecommendation recommendation = getOwnedOrThrow(memberId, recommendationId);
		requireActionable(recommendation);
		recommendation.view(Instant.now());
		return ExperimentRecommendationResponse.of(recommendation);
	}

	@Transactional
	public ExperimentRecommendationResponse dismiss(UUID memberId, UUID recommendationId) {
		ExperimentRecommendation recommendation = getOwnedOrThrow(memberId, recommendationId);
		requireActionable(recommendation);
		recommendation.dismiss(Instant.now());
		return ExperimentRecommendationResponse.of(recommendation);
	}

	private void generateRuleBasedRecommendations(UUID memberId) {
		Member member = memberRepository.getReferenceById(memberId);
		Set<UUID> notAFitMissionIds = experimentMissionRecordRepository
				.findMissionIdsByMemberAndAttemptStatus(memberId, ExperimentAttemptStatus.NOT_A_FIT);
		Instant now = Instant.now();
		Instant expiresAt = now.plus(RECOMMENDATION_FRESHNESS);

		if (!userExperimentProgramRepository.existsByMember_IdAndStartedAtIsNotNull(memberId)) {
			createRecommendationIfFresh(
					member, "NOW_MIND_3", ExperimentRecommendationSourceType.RULE, null,
					REASON_FIRST_TIME, FIRST_TIME_REASON_TEXT, null, notAFitMissionIds, now, expiresAt);
			createRecommendationIfFresh(
					member, "KNOW_ME_LIGHTLY_7", ExperimentRecommendationSourceType.RULE, null,
					REASON_FIRST_TIME, FIRST_TIME_REASON_TEXT, null, notAFitMissionIds, now, expiresAt);
			return;
		}

		List<CheckIn> lowEnergyCheckIns = checkInRepository.findTop5ByMember_IdOrderByCheckInDateDesc(memberId).stream()
				.filter(checkIn -> checkIn.getEnergyLevel() != null && checkIn.getEnergyLevel() <= LOW_ENERGY_THRESHOLD)
				.toList();
		if (lowEnergyCheckIns.size() >= LOW_ENERGY_MIN_OCCURRENCES) {
			CheckIn latest = lowEnergyCheckIns.get(0);
			String reasonText = "최근 체크인에서 에너지가 낮다고 기록된 날이 여러 번 있었어요. "
					+ "원한다면 '작은 회복 알아보기'를 살펴볼 수 있어요. 지금은 살펴보지 않아도 괜찮아요.";
			String evidence = toJson(new LowEnergyEvidence(
					lowEnergyCheckIns.stream().map(checkIn -> checkIn.getCheckInDate().toString()).toList(),
					lowEnergyCheckIns.stream().map(CheckIn::getEnergyLevel).toList()));
			createRecommendationIfFresh(
					member, "RECOVERY_CLUES_3", ExperimentRecommendationSourceType.CHECK_IN, latest.getEntry(),
					REASON_LOW_ENERGY_REPEATED, reasonText, evidence, notAFitMissionIds, now, expiresAt);
		}
	}

	private void createRecommendationIfFresh(
			Member member, String programCode, ExperimentRecommendationSourceType sourceType, Entry sourceEntry,
			String reasonCode, String reasonText, String evidence, Set<UUID> notAFitMissionIds, Instant now, Instant expiresAt) {
		ExperimentProgram program = experimentProgramRepository
				.findByCodeAndStatus(programCode, ExperimentProgramStatus.PUBLISHED)
				.orElseThrow();
		if (isDeprioritizedByNotAFit(program, notAFitMissionIds)) {
			return;
		}
		boolean alreadyRecommendedRecently = experimentRecommendationRepository
				.existsByMember_IdAndProgram_IdAndSourceTypeAndReasonCodeAndCreatedAtAfter(
						member.getId(), program.getId(), sourceType, reasonCode, now.minus(RECOMMENDATION_FRESHNESS));
		if (alreadyRecommendedRecently) {
			return;
		}
		experimentRecommendationRepository.save(ExperimentRecommendation.create(
				member, program, sourceType, sourceEntry, reasonCode, reasonText, evidence, expiresAt));
	}

	// §14 근거 최소 기준: 사용자가 NOT_A_FIT으로 기록한 미션이 코스 미션의 절반을 넘으면 우선순위를
	// 낮춘다(Beta 1에서는 아예 추천하지 않는 것으로 단순화).
	private boolean isDeprioritizedByNotAFit(ExperimentProgram program, Set<UUID> notAFitMissionIds) {
		if (notAFitMissionIds.isEmpty()) {
			return false;
		}
		List<ExperimentProgramMission> missions =
				experimentProgramMissionRepository.findByProgram_IdOrderById_DayNumberAsc(program.getId());
		if (missions.isEmpty()) {
			return false;
		}
		long notAFitCount = missions.stream()
				.filter(programMission -> notAFitMissionIds.contains(programMission.getMission().getId()))
				.count();
		return notAFitCount * 2 > missions.size();
	}

	private void requireActionable(ExperimentRecommendation recommendation) {
		if (RESPONDED_STATUSES.contains(recommendation.getStatus())) {
			throw new BusinessException(ExperimentErrorCode.RECOMMENDATION_NOT_ACTIONABLE);
		}
	}

	private ExperimentRecommendation getOwnedOrThrow(UUID memberId, UUID recommendationId) {
		return experimentRecommendationRepository.findByIdAndMember_Id(recommendationId, memberId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.RECOMMENDATION_NOT_FOUND));
	}

	private String toJson(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("추천 근거 데이터를 직렬화할 수 없습니다.", e);
		}
	}

	private record LowEnergyEvidence(List<String> checkInDates, List<Short> energyLevels) {
	}

}
