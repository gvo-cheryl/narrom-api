package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.lifetime.dto.EntryTimelineResponse;
import com.naroom.api.record.EntrySelfReflectionService;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class EntryTimelineServiceTest {

	@Autowired
	private EntryTimelineService entryTimelineService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private AiJobService aiJobService;

	@Autowired
	private EntrySelfReflectionService entrySelfReflectionService;

	@Test
	void getTimeline_includesConfirmedTagsButNotSuggested() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag confirmedTag = tagRepository.save(
				Tag.createSystemTag(TagCategory.EMOTION, "편안함-" + System.nanoTime(), "편안함-" + System.nanoTime()));
		Tag suggestedTag = tagRepository.save(
				Tag.createSystemTag(TagCategory.EMOTION, "답답함-" + System.nanoTime(), "답답함-" + System.nanoTime()));
		entryTagRepository.save(EntryTag.attachByUser(entry, confirmedTag));
		entryTagRepository.save(EntryTag.suggestByAi(entry, suggestedTag, null, null, null, null));

		List<EntryTimelineResponse> timeline =
				entryTimelineService.getTimeline(member.getId(), null, null, null);

		assertEquals(1, timeline.size());
		assertEquals(1, timeline.get(0).tags().size());
		assertEquals(confirmedTag.getId(), timeline.get(0).tags().get(0).tag().id());
	}

	@Test
	void getTimeline_includesLatestAiStatusAndSelfReflectionFlag() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		aiJobService.createForEntry(member.getId(), AiFeatureType.ENTRY_REFLECTION, entry.getId(), "key-" + System.nanoTime());
		entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "내 생각", null);

		List<EntryTimelineResponse> timeline =
				entryTimelineService.getTimeline(member.getId(), null, null, null);

		assertEquals(1, timeline.size());
		assertEquals(AiJobStatus.PENDING, timeline.get(0).aiStatus());
		assertTrue(timeline.get(0).hasSelfReflection());
	}

	@Test
	void getTimeline_entryWithoutAiJob_returnsNullAiStatus() {
		Member member = memberRepository.save(Member.create("지연"));
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		List<EntryTimelineResponse> timeline =
				entryTimelineService.getTimeline(member.getId(), null, null, null);

		assertEquals(1, timeline.size());
		assertNull(timeline.get(0).aiStatus());
		assertTrue(timeline.get(0).tags().isEmpty());
	}

	@Test
	void getTimeline_filtersByDateRangeAndEntryType() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate today = LocalDate.now();
		LocalDate lastWeek = today.minusDays(7);
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "A", today, null, null, null));
		entryRepository.save(Entry.create(member, EntryType.GRATITUDE, null, "B", today, null, null, null));
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "C", lastWeek, null, null, null));

		List<EntryTimelineResponse> withinRange =
				entryTimelineService.getTimeline(member.getId(), today.minusDays(1), today, null);
		List<EntryTimelineResponse> byType =
				entryTimelineService.getTimeline(member.getId(), null, null, EntryType.GRATITUDE);

		assertEquals(2, withinRange.size());
		assertEquals(1, byType.size());
	}

}
