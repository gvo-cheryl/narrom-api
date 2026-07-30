package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.PeriodCalculator.Period;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionEntryRepository;
import com.naroom.api.lifetime.domain.repository.PeriodReflectionRepository;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 3-C: 기간 계산(3-A)·자격 확인(3-A)을 거쳐 봉투 Entry·PeriodReflection·근거 기록 연결을 만들고 AiJob을
// 생성한다. 같은 기간(feature_type+periodStart)에 이미 회고가 있고 아직 끝나지 않았거나(진행 중) 이미
// 완료됐으면 새로 만들지 않고 그대로 반환한다 - "주차당 1개"(§6.1)를 재요청에도 그대로 지킨다. 다만 기존
// 회고가 영구 실패(FAILED)한 상태라면 PeriodReflection.regenerate()로 새 버전을 만들어 재시도한다 -
// 그렇지 않으면 처리 중 예외가 한 번 나는 순간 그 기간의 회고를 영원히 다시 만들 수 없게 된다.
@Service
@Transactional(readOnly = true)
public class PeriodReflectionService {

	private final MemberRepository memberRepository;
	private final EntryRepository entryRepository;
	private final PeriodReflectionRepository periodReflectionRepository;
	private final PeriodReflectionEntryRepository periodReflectionEntryRepository;
	private final PeriodReflectionEligibilityService eligibilityService;
	private final AiJobService aiJobService;

	public PeriodReflectionService(
			MemberRepository memberRepository,
			EntryRepository entryRepository,
			PeriodReflectionRepository periodReflectionRepository,
			PeriodReflectionEntryRepository periodReflectionEntryRepository,
			PeriodReflectionEligibilityService eligibilityService,
			AiJobService aiJobService) {
		this.memberRepository = memberRepository;
		this.entryRepository = entryRepository;
		this.periodReflectionRepository = periodReflectionRepository;
		this.periodReflectionEntryRepository = periodReflectionEntryRepository;
		this.eligibilityService = eligibilityService;
		this.aiJobService = aiJobService;
	}

	@Transactional
	public PeriodReflection generate(UUID memberId, AiFeatureType featureType) {
		if (featureType != AiFeatureType.WEEKLY_REFLECTION && featureType != AiFeatureType.THREE_DAY_REFLECTION) {
			throw new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_FEATURE_TYPE_INVALID);
		}
		Member member = memberRepository.getReferenceById(memberId);
		Period period = PeriodCalculator.compute(featureType, ZoneId.of(member.getTimezone()));

		List<PeriodReflection> existing = periodReflectionRepository
				.findByMember_IdAndFeatureTypeAndPeriodStartOrderByVersionNoDesc(memberId, featureType, period.start());
		PeriodReflection previous = existing.isEmpty() ? null : existing.get(0);
		if (previous != null && previous.getStatus() != AiJobStatus.FAILED) {
			return previous;
		}

		List<Entry> evidenceEntries =
				eligibilityService.selectEvidenceEntriesOrThrow(memberId, featureType, period.start(), period.end());

		Entry envelopeEntry = entryRepository.save(
				Entry.create(member, entryTypeFor(featureType), null, null, period.end(), null, null, null));
		envelopeEntry.publish();

		PeriodReflection periodReflection = periodReflectionRepository.save(previous == null
				? PeriodReflection.request(member, envelopeEntry, featureType, period.start(), period.end())
				: PeriodReflection.regenerate(member, envelopeEntry, previous));

		for (Entry evidenceEntry : evidenceEntries) {
			periodReflectionEntryRepository.save(PeriodReflectionEntry.link(periodReflection, evidenceEntry, null));
		}

		aiJobService.createForEntry(
				memberId, featureType, envelopeEntry.getId(),
				"period-reflection-" + featureType + "-" + period.start() + "-v" + periodReflection.getVersionNo());

		return periodReflection;
	}

	public PeriodReflection getOwnedOrThrow(UUID memberId, UUID periodReflectionId) {
		return periodReflectionRepository.findByIdAndMember_Id(periodReflectionId, memberId)
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
	}

	// 지난 회고 목록(히스토리). 재생성으로 같은 기간에 버전이 여러 개 쌓일 수 있어 periodStart+featureType당
	// 최신 버전 하나만 남긴다 - 실패해서 재생성된 이전 버전들은 목록에 노출하지 않는다.
	public List<PeriodReflection> listRecent(UUID memberId, AiFeatureType featureType) {
		List<PeriodReflection> all = featureType == null
				? periodReflectionRepository.findByMember_IdOrderByPeriodStartDescVersionNoDesc(memberId)
				: periodReflectionRepository.findByMember_IdAndFeatureTypeOrderByPeriodStartDescVersionNoDesc(memberId, featureType);

		Map<String, PeriodReflection> latestByPeriod = new LinkedHashMap<>();
		for (PeriodReflection reflection : all) {
			String key = reflection.getFeatureType() + "|" + reflection.getPeriodStart();
			latestByPeriod.putIfAbsent(key, reflection);
		}
		return new ArrayList<>(latestByPeriod.values());
	}

	// PeriodReflectionJobProcessor가 작업을 영구 실패로 종결할 때 함께 호출한다. ai_jobs 상태만 바뀌고
	// period_reflections.status는 그대로 두면, 프론트가 이 회고를 다시 폴링할 때마다 영원히 PENDING으로
	// 보여서 "정리 중"인 것처럼 보이고, generate()도 이미 있는 회고로 오인해 재시도를 만들지 않는다.
	@Transactional
	public void markFailed(UUID entryId, String errorCode) {
		PeriodReflection periodReflection = periodReflectionRepository.findByEntry_Id(entryId)
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
		periodReflection.fail(errorCode, Instant.now());
	}

	// PeriodReflectionJobProcessor(AI 잡 처리)가 이 메서드 밖에서 member·evidenceEntries에
	// 접근해도 안전하도록, join fetch 조회로 지연 로딩 프록시가 아닌 실제 값을 담아 반환한다.
	public ProcessingContext loadForProcessing(UUID entryId) {
		PeriodReflection periodReflection = periodReflectionRepository.findByEntry_IdWithMember(entryId)
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
		List<Entry> evidenceEntries = periodReflectionEntryRepository
				.findByPeriodReflection_IdWithEntry(periodReflection.getId()).stream()
				.map(PeriodReflectionEntry::getEntry)
				.toList();
		return new ProcessingContext(periodReflection, evidenceEntries);
	}

	public record ProcessingContext(PeriodReflection periodReflection, List<Entry> evidenceEntries) {
	}

	private EntryType entryTypeFor(AiFeatureType featureType) {
		return switch (featureType) {
			case WEEKLY_REFLECTION -> EntryType.WEEKLY_REFLECTION;
			case THREE_DAY_REFLECTION -> EntryType.THREE_DAY_REFLECTION;
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

}
