package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.dto.TagDistributionResponse;
import com.naroom.api.record.EntryTagService;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import com.naroom.api.record.dto.EntryTagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class TagExplorationServiceTest {

	@Autowired
	private TagExplorationService tagExplorationService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private EntryTagService entryTagService;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Test
	void getDistribution_countsConfirmedTagsAndExcludesSuggested() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entryA = entryRepository.save(Entry.create(member, EntryType.FREE, null, "A", LocalDate.now(), null, null, null));
		Entry entryB = entryRepository.save(Entry.create(member, EntryType.FREE, null, "B", LocalDate.now(), null, null, null));
		Tag emotionTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "서운함", "서운함" + System.nanoTime()));
		Tag suggestedTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "제안됨", "제안됨" + System.nanoTime()));
		EntryTagResponse attachedA = entryTagService.attachUserTag(member.getId(), entryA.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), entryA.getId(), attachedA.id());
		EntryTagResponse attachedB = entryTagService.attachUserTag(member.getId(), entryB.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), entryB.getId(), attachedB.id());
		entryTagRepository.save(EntryTag.suggestByAi(entryA, suggestedTag, null, null, null, null));

		List<TagDistributionResponse> distribution = tagExplorationService.getDistribution(member.getId(), null);

		assertEquals(1, distribution.size());
		assertEquals(emotionTag.getId(), distribution.get(0).tagId());
		assertEquals(2, distribution.get(0).count());
	}

	@Test
	void getDistribution_filtersByCategory() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag emotionTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "안도감", "안도감" + System.nanoTime()));
		Tag situationTag = tagRepository.save(Tag.createUserTag(member, TagCategory.SITUATION, "직장", "직장" + System.nanoTime()));
		EntryTagResponse attachedEmotion = entryTagService.attachUserTag(member.getId(), entry.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), entry.getId(), attachedEmotion.id());
		EntryTagResponse attachedSituation = entryTagService.attachUserTag(member.getId(), entry.getId(), situationTag.getId());
		entryTagService.confirmTag(member.getId(), entry.getId(), attachedSituation.id());

		List<TagDistributionResponse> distribution = tagExplorationService.getDistribution(member.getId(), TagCategory.EMOTION);

		assertEquals(1, distribution.size());
		assertTrue(distribution.stream().allMatch(d -> d.category() == TagCategory.EMOTION));
	}

	@Test
	void getDistribution_withRange_excludesTagsOutsideRange() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry recentEntry =
				entryRepository.save(Entry.create(member, EntryType.FREE, null, "최근", LocalDate.now(), null, null, null));
		Entry oldEntry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "오래됨", LocalDate.now().minusDays(20), null, null, null));
		Tag emotionTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "안도감", "안도감" + System.nanoTime()));
		EntryTagResponse attachedRecent = entryTagService.attachUserTag(member.getId(), recentEntry.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), recentEntry.getId(), attachedRecent.id());
		EntryTagResponse attachedOld = entryTagService.attachUserTag(member.getId(), oldEntry.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), oldEntry.getId(), attachedOld.id());

		List<TagDistributionResponse> distribution = tagExplorationService.getDistribution(member.getId(), null, 7);

		assertEquals(1, distribution.size());
		assertEquals(1, distribution.get(0).count());
	}

	@Test
	void getDistribution_withNullRange_matchesAllTimeOverload() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag emotionTag = tagRepository.save(Tag.createUserTag(member, TagCategory.EMOTION, "고마움", "고마움" + System.nanoTime()));
		EntryTagResponse attached = entryTagService.attachUserTag(member.getId(), entry.getId(), emotionTag.getId());
		entryTagService.confirmTag(member.getId(), entry.getId(), attached.id());

		List<TagDistributionResponse> withoutRange = tagExplorationService.getDistribution(member.getId(), null);
		List<TagDistributionResponse> withNullRange = tagExplorationService.getDistribution(member.getId(), null, null);

		assertEquals(withoutRange, withNullRange);
	}

	@Test
	void getDistribution_invalidRange_throwsBusinessException() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> tagExplorationService.getDistribution(member.getId(), null, 10));
		assertEquals(LifetimeErrorCode.ANALYTICS_RANGE_INVALID, exception.errorCode());
	}

}
