package com.naroom.api.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.experiment.domain.entity.UserExperimentProgram;
import com.naroom.api.experiment.domain.entity.UserExperimentProgramStatus;
import com.naroom.api.experiment.domain.error.ExperimentErrorCode;
import com.naroom.api.experiment.domain.repository.ExperimentMissionRecordRepository;
import com.naroom.api.experiment.domain.repository.UserExperimentProgramRepository;
import com.naroom.api.experiment.dto.ExperimentAiJobSummary;
import com.naroom.api.experiment.dto.ExperimentCourseReviewRequest;
import com.naroom.api.experiment.dto.ExperimentCourseReviewResponse;
import com.naroom.api.experiment.dto.ExperimentEndEarlyResponse;
import com.naroom.api.experiment.dto.ExperimentPastProgramResponse;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.PeriodReflectionService;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// §5.4 코스 종료: 코스 돌아보기 저장(AWAITING_REVIEW -> COMPLETED), 지금까지 기록하고 마무리하기
// (-> ENDED_EARLY), 지난 작은 실험 목록을 담당한다.
@Service
@Transactional(readOnly = true)
public class ExperimentReviewService {

	private static final Set<UserExperimentProgramStatus> ENDED_STATUSES =
			Set.of(UserExperimentProgramStatus.COMPLETED, UserExperimentProgramStatus.ENDED_EARLY);

	// ExperimentProgressService와 동일한 이유(§12.3 자유 JSON 저장) - 이 프로젝트는 ObjectMapper를
	// 스프링 빈으로 노출하지 않는다.
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final MemberRepository memberRepository;
	private final UserExperimentProgramRepository userExperimentProgramRepository;
	private final ExperimentMissionRecordRepository experimentMissionRecordRepository;
	private final EntryRepository entryRepository;
	private final PeriodReflectionService periodReflectionService;

	public ExperimentReviewService(
			MemberRepository memberRepository,
			UserExperimentProgramRepository userExperimentProgramRepository,
			ExperimentMissionRecordRepository experimentMissionRecordRepository,
			EntryRepository entryRepository,
			PeriodReflectionService periodReflectionService) {
		this.memberRepository = memberRepository;
		this.userExperimentProgramRepository = userExperimentProgramRepository;
		this.experimentMissionRecordRepository = experimentMissionRecordRepository;
		this.entryRepository = entryRepository;
		this.periodReflectionService = periodReflectionService;
	}

	@Transactional
	public ExperimentCourseReviewResponse completeReview(
			UUID memberId, UUID userExperimentProgramId, ExperimentCourseReviewRequest request) {
		UserExperimentProgram program = getOwnedProgramOrThrow(memberId, userExperimentProgramId);
		if (program.getStatus() != UserExperimentProgramStatus.AWAITING_REVIEW) {
			throw new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_AWAITING_REVIEW);
		}

		Member member = memberRepository.getReferenceById(memberId);
		LocalDate today = LocalDate.now(ZoneId.of(member.getTimezone()));

		Entry reviewEntry = entryRepository.save(Entry.create(
				member, EntryType.EXPERIMENT_REVIEW, program.getTitleSnapshot(), request.discovery(), today, null, null, null));
		reviewEntry.linkExperimentProgram(program.getId());
		reviewEntry.publish();

		Instant now = Instant.now();
		program.complete(toJson(new ReviewData(request)), request.userSummary(), reviewEntry, now);

		ExperimentAiJobSummary aiJobSummary = Boolean.TRUE.equals(request.requestAiReflection())
				? requestAiReflectionIfEligible(memberId, member, program, today)
				: null;

		return new ExperimentCourseReviewResponse(program.getStatus(), true, aiJobSummary);
	}

	// §11.3: 3일 코스에서만 코스 전용 AI 회고를 만든다. 근거 기록은 이 코스에서 만들어진(엔트리가 연결된)
	// 미션 기록으로 한정한다 - 같은 기간에 남긴 무관한 자유 기록까지 섞이지 않게 하기 위함이다.
	private ExperimentAiJobSummary requestAiReflectionIfEligible(
			UUID memberId, Member member, UserExperimentProgram program, LocalDate today) {
		if (program.getDurationDays() != 3) {
			return new ExperimentAiJobSummary(
					null, null, "3일 코스에서만 생성합니다. 7일 코스는 Beta 1 정책에 따라 일반 코스 회고로 저장합니다.");
		}

		List<Entry> evidenceEntries = experimentMissionRecordRepository
				.findByUserExperimentProgram_IdOrderByRecordDateDesc(program.getId()).stream()
				.map(missionRecord -> missionRecord.getEntry())
				.filter(Objects::nonNull)
				.toList();
		LocalDate periodStart = program.getStartedAt() != null
				? program.getStartedAt().atZone(ZoneId.of(member.getTimezone())).toLocalDate()
				: today;

		PeriodReflection reflection = periodReflectionService.generateForExperimentCourse(
				memberId, program.getId(), periodStart, today, evidenceEntries);
		return new ExperimentAiJobSummary(AiFeatureType.THREE_DAY_REFLECTION, reflection.getStatus(), null);
	}

	@Transactional
	public ExperimentEndEarlyResponse endEarly(UUID memberId, UUID userExperimentProgramId) {
		UserExperimentProgram program = getOwnedProgramOrThrow(memberId, userExperimentProgramId);
		if (ENDED_STATUSES.contains(program.getStatus())) {
			throw new BusinessException(ExperimentErrorCode.USER_PROGRAM_ALREADY_ENDED);
		}
		program.endEarly(Instant.now());
		return new ExperimentEndEarlyResponse(program.getId(), program.getStatus());
	}

	public List<ExperimentPastProgramResponse> listPast(UUID memberId) {
		return userExperimentProgramRepository.findByMember_IdOrderByCreatedAtDesc(memberId).stream()
				.filter(program -> ENDED_STATUSES.contains(program.getStatus()))
				.map(ExperimentPastProgramResponse::from)
				.toList();
	}

	private UserExperimentProgram getOwnedProgramOrThrow(UUID memberId, UUID userExperimentProgramId) {
		return userExperimentProgramRepository.findByIdAndMember_Id(userExperimentProgramId, memberId)
				.orElseThrow(() -> new BusinessException(ExperimentErrorCode.USER_PROGRAM_NOT_FOUND));
	}

	private String toJson(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("코스 돌아보기 데이터를 직렬화할 수 없습니다.", e);
		}
	}

	private record ReviewData(
			Short mostMemorableDay, Short leastBurdensomeDay, Short notFitDay,
			List<String> helpfulConditions, List<String> difficultConditions,
			String discovery, String continueAction) {

		private ReviewData(ExperimentCourseReviewRequest request) {
			this(
					request.mostMemorableDay(), request.leastBurdensomeDay(), request.notFitDay(),
					request.helpfulConditions(), request.difficultConditions(),
					request.discovery(), request.continueAction());
		}
	}

}
