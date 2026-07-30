package com.naroom.api.lifetime;

import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.dto.TagDistributionResponse;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagState;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 5단계(태그 탐색): 확정(CONFIRMED) 또는 시스템 적용(SYSTEM) 태그만 센다 - 아직 확인 전인 AI 제안(SUGGESTED)을
// 사용자의 확정된 경향처럼 보여주지 않는다(EntryTimelineService와 같은 원칙). "관련 기록" 목록은
// EntryTimelineService.getByTag()가 담당하고, 여기서는 분포(개수) 집계만 다룬다. "동시 등장" 분석은
// 이번 1차 구현에서는 하지 않는다 - 다음 반복 과제로 남긴다.
@Service
@Transactional(readOnly = true)
public class TagExplorationService {

	private static final Set<TagState> CONFIRMED_TAG_STATES = EnumSet.of(TagState.CONFIRMED, TagState.SYSTEM);
	private static final Set<Integer> ALLOWED_RANGE_DAYS = Set.of(7, 14, 30);

	private final EntryTagRepository entryTagRepository;

	public TagExplorationService(EntryTagRepository entryTagRepository) {
		this.entryTagRepository = entryTagRepository;
	}

	public List<TagDistributionResponse> getDistribution(UUID memberId, TagCategory category) {
		return summarize(entryTagRepository.findByEntry_Member_IdAndStateIn(memberId, CONFIRMED_TAG_STATES), category);
	}

	// LifeTime 홈/키워드 탐색(L03/L04/L10)이 선택한 기간(7/14/30일)만으로 분포를 좁혀 볼 때 쓴다.
	// rangeDays가 없으면(null) 기존과 동일하게 전체 기간을 집계한다 - 이미 range 없이 호출하는 곳과 호환된다.
	public List<TagDistributionResponse> getDistribution(UUID memberId, TagCategory category, Integer rangeDays) {
		if (rangeDays == null) {
			return getDistribution(memberId, category);
		}
		if (!ALLOWED_RANGE_DAYS.contains(rangeDays)) {
			throw new BusinessException(LifetimeErrorCode.ANALYTICS_RANGE_INVALID);
		}
		LocalDate end = LocalDate.now();
		LocalDate start = end.minusDays(rangeDays - 1L);
		return summarize(
				entryTagRepository.findByEntry_Member_IdAndStateInAndEntry_RecordDateBetween(
						memberId, CONFIRMED_TAG_STATES, start, end),
				category);
	}

	private List<TagDistributionResponse> summarize(List<EntryTag> entryTags, TagCategory category) {
		return entryTags.stream()
				.map(EntryTag::getTag)
				.filter(tag -> category == null || tag.getCategory() == category)
				.collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
				.entrySet().stream()
				.map(entry -> new TagDistributionResponse(
						entry.getKey().getId(), entry.getKey().getName(), entry.getKey().getCategory(), entry.getValue()))
				.sorted(Comparator.comparing(TagDistributionResponse::count).reversed())
				.toList();
	}

}
