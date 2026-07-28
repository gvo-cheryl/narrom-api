package com.naroom.api.lifetime;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 3단계 3-A: 기간별 회고의 최소 기록 수(2026-07-28 결정 - 주간 3건/3일 1건, PRD §13의 열린 질문에 대한 답)를
// 확인하고 근거 기록을 고른다. §12.4의 정교한 선별 기준(변화량 큰 기록, 편향 방지 무작위 표본 등)은 이번
// 1차 구현에서는 적용하지 않고 "기간 내 발행된 기록 전부"로 단순화한다 - 다음 반복 과제로 남겨둔다.
@Service
@Transactional(readOnly = true)
public class PeriodReflectionEligibilityService {

	// 회고 봉투 자체(WEEKLY_REFLECTION/THREE_DAY_REFLECTION)와 나의 정리(SELF_SUMMARY)는 근거 기록에서
	// 제외한다 - 회고가 회고 자신이나 다른 회고를 근거로 삼는 순환을 막기 위함이다.
	private static final Set<EntryType> EXCLUDED_EVIDENCE_TYPES =
			EnumSet.of(EntryType.WEEKLY_REFLECTION, EntryType.THREE_DAY_REFLECTION, EntryType.SELF_SUMMARY);

	private final EntryRepository entryRepository;

	public PeriodReflectionEligibilityService(EntryRepository entryRepository) {
		this.entryRepository = entryRepository;
	}

	public List<Entry> selectEvidenceEntriesOrThrow(
			UUID memberId, AiFeatureType featureType, LocalDate periodStart, LocalDate periodEnd) {
		List<Entry> evidenceEntries = entryRepository
				.findByMember_IdAndStatusAndRecordDateBetweenOrderByRecordDateAscCreatedAtAsc(
						memberId, EntryStatus.PUBLISHED, periodStart, periodEnd)
				.stream()
				.filter(entry -> !EXCLUDED_EVIDENCE_TYPES.contains(entry.getEntryType()))
				.toList();
		if (evidenceEntries.size() < minimumRecordCount(featureType)) {
			throw new BusinessException(LifetimeErrorCode.PERIOD_REFLECTION_INSUFFICIENT_RECORDS);
		}
		return evidenceEntries;
	}

	private static int minimumRecordCount(AiFeatureType featureType) {
		return switch (featureType) {
			case WEEKLY_REFLECTION -> 3;
			case THREE_DAY_REFLECTION -> 1;
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

}
