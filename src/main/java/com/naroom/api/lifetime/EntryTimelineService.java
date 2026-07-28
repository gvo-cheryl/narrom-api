package com.naroom.api.lifetime;

import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.lifetime.dto.EntryTimelineResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.TagState;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.dto.EntryTagResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// §공백 8(coverage-checklist L): 타임라인은 원문뿐 아니라 태그·AI 상태·자기정리 여부까지 한 번에 보여줘야
// 하지만, 이 집계를 기존 EntryResponse(create/update/publish 등에서도 쓰는 가벼운 응답)에 얹으면 그 단순한
// 응답들까지 매번 태그·AI작업·자기회고를 조회하게 된다. 그래서 타임라인 전용 응답·조회 경로를 새로 둔다.
// 태그는 SUGGESTED(AI가 제안했지만 아직 사용자가 확인 안 함)를 목록에서 제외한다 - 타임라인은 훑어보는
// 화면이라 확인/거부 UI 없이 AI 제안을 확정된 사실처럼 보여주면 안 된다(§9.4/§25 "AI 결과를 동의·확인 가능").
@Service
@Transactional(readOnly = true)
public class EntryTimelineService {

	private static final Set<TagState> TIMELINE_TAG_STATES = EnumSet.of(TagState.CONFIRMED, TagState.SYSTEM);

	private final EntryRepository entryRepository;
	private final EntryTagRepository entryTagRepository;
	private final AiJobRepository aiJobRepository;
	private final EntrySelfReflectionRepository entrySelfReflectionRepository;

	public EntryTimelineService(
			EntryRepository entryRepository,
			EntryTagRepository entryTagRepository,
			AiJobRepository aiJobRepository,
			EntrySelfReflectionRepository entrySelfReflectionRepository) {
		this.entryRepository = entryRepository;
		this.entryTagRepository = entryTagRepository;
		this.aiJobRepository = aiJobRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
	}

	public List<EntryTimelineResponse> getTimeline(UUID memberId, LocalDate from, LocalDate to, EntryType entryType) {
		return buildResponses(resolveEntries(memberId, from, to, entryType));
	}

	// 5단계 태그 탐색: 특정 태그가 붙은 기록 목록도 타임라인과 같은 카드 형태(EntryTimelineResponse)로 보여준다.
	public List<EntryTimelineResponse> getByTag(UUID memberId, UUID tagId) {
		List<UUID> entryIds = entryTagRepository.findByEntry_Member_IdAndTag_IdAndStateIn(memberId, tagId, TIMELINE_TAG_STATES).stream()
				.map(entryTag -> entryTag.getEntry().getId())
				.toList();
		if (entryIds.isEmpty()) {
			return List.of();
		}
		return buildResponses(entryRepository.findByIdInOrderByRecordDateDescCreatedAtDesc(entryIds));
	}

	private List<EntryTimelineResponse> buildResponses(List<Entry> entries) {
		if (entries.isEmpty()) {
			return List.of();
		}
		List<UUID> entryIds = entries.stream().map(Entry::getId).toList();

		Map<UUID, List<EntryTagResponse>> tagsByEntry = entryTagRepository
				.findByEntry_IdInAndStateIn(entryIds, TIMELINE_TAG_STATES).stream()
				.collect(Collectors.groupingBy(
						entryTag -> entryTag.getEntry().getId(),
						Collectors.mapping(EntryTagResponse::from, Collectors.toList())));

		// 오름차순으로 가져와 같은 entry_id를 나중 값(더 최근 작업)이 덮어쓰게 한다.
		Map<UUID, AiJobStatus> aiStatusByEntry = new LinkedHashMap<>();
		for (AiJob job : aiJobRepository.findByEntry_IdInOrderByCreatedAtAsc(entryIds)) {
			aiStatusByEntry.put(job.getEntry().getId(), job.getStatus());
		}

		Set<UUID> entriesWithSelfReflection = entrySelfReflectionRepository.findByEntry_IdIn(entryIds).stream()
				.map(reflection -> reflection.getEntry().getId())
				.collect(Collectors.toSet());

		return entries.stream()
				.map(entry -> EntryTimelineResponse.of(
						entry,
						tagsByEntry.getOrDefault(entry.getId(), List.of()),
						aiStatusByEntry.get(entry.getId()),
						entriesWithSelfReflection.contains(entry.getId())))
				.collect(Collectors.toList());
	}

	private List<Entry> resolveEntries(UUID memberId, LocalDate from, LocalDate to, EntryType entryType) {
		if (from != null && to != null) {
			if (entryType != null) {
				return entryRepository.findByMember_IdAndEntryTypeAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(
						memberId, entryType, from, to);
			}
			return entryRepository.findByMember_IdAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(memberId, from, to);
		}
		if (entryType != null) {
			return entryRepository.findByMember_IdAndEntryTypeOrderByRecordDateDescCreatedAtDesc(memberId, entryType);
		}
		return entryRepository.findByMember_IdOrderByRecordDateDescCreatedAtDesc(memberId);
	}

}
