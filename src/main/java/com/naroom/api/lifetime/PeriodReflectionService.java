package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
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

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

// 3-C: 기간 계산(3-A)·자격 확인(3-A)을 거쳐 봉투 Entry·PeriodReflection·근거 기록 연결을 만들고 AiJob을
// 생성한다. 같은 기간(feature_type+periodStart)에 이미 회고가 있으면(어떤 상태든) 새로 만들지 않고 그대로
// 반환한다 - "주차당 1개"(§6.1)를 재요청에도 그대로 지킨다. 재생성은 이 메서드가 아니라 별도 액션이다.
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
		if (!existing.isEmpty()) {
			return existing.get(0);
		}

		List<Entry> evidenceEntries =
				eligibilityService.selectEvidenceEntriesOrThrow(memberId, featureType, period.start(), period.end());

		Entry envelopeEntry = entryRepository.save(
				Entry.create(member, entryTypeFor(featureType), null, null, period.end(), null, null, null));
		envelopeEntry.publish();

		PeriodReflection periodReflection = periodReflectionRepository.save(
				PeriodReflection.request(member, envelopeEntry, featureType, period.start(), period.end()));

		for (Entry evidenceEntry : evidenceEntries) {
			periodReflectionEntryRepository.save(PeriodReflectionEntry.link(periodReflection, evidenceEntry, null));
		}

		aiJobService.createForEntry(
				memberId, featureType, envelopeEntry.getId(),
				"period-reflection-" + featureType + "-" + period.start());

		return periodReflection;
	}

	public PeriodReflection getOwnedOrThrow(UUID memberId, UUID periodReflectionId) {
		return periodReflectionRepository.findByIdAndMember_Id(periodReflectionId, memberId)
				.orElseThrow(() -> new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_NOT_FOUND));
	}

	private EntryType entryTypeFor(AiFeatureType featureType) {
		return switch (featureType) {
			case WEEKLY_REFLECTION -> EntryType.WEEKLY_REFLECTION;
			case THREE_DAY_REFLECTION -> EntryType.THREE_DAY_REFLECTION;
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

}
