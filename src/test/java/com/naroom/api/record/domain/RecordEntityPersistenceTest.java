package com.naroom.api.record.domain;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.record.domain.entity.EmotionTagTopic;
import com.naroom.api.record.domain.entity.EmotionTagTopicLink;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntrySelfReflection;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import com.naroom.api.record.domain.entity.TagSource;
import com.naroom.api.record.domain.entity.TagState;
import com.naroom.api.record.domain.repository.EmotionTagTopicLinkRepository;
import com.naroom.api.record.domain.repository.EmotionTagTopicRepository;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Postgres native enum(entry_type/tag_scope 등), Entry의 자기 참조 parent_entry_id,
 * emotion_tag_topic_links의 공유 PK(tag_id)처럼 스키마 검증만으로는 확인되지 않는
 * 실제 저장/조회 왕복을 검증한다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class RecordEntityPersistenceTest {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private EntrySelfReflectionRepository entrySelfReflectionRepository;

	@Autowired
	private EmotionTagTopicRepository emotionTagTopicRepository;

	@Autowired
	private EmotionTagTopicLinkRepository emotionTagTopicLinkRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void recordAggregate_roundTripsThroughAllTables() {
		Member member = memberRepository.save(Member.create("지연"));

		Tag userTag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.EMOTION, "테스트감정", "테스트감정" + System.nanoTime()));

		Entry parent = entryRepository.save(
				Entry.create(member, EntryType.FREE, "부모 기록", "본문", LocalDate.now(), null, null, null));
		Entry child = entryRepository.save(
				Entry.create(member, EntryType.FREE, "이어쓰기", "본문2", LocalDate.now(), parent, null, null));
		child.publish();
		entryRepository.saveAndFlush(child);

		EntryTag entryTag = entryTagRepository.save(EntryTag.attachByUser(child, userTag));

		EntrySelfReflection reflection =
				entrySelfReflectionRepository.save(EntrySelfReflection.create(child, "내가 느낀 것"));

		entityManager.flush();
		entityManager.clear();

		Entry reloadedChild = entryRepository.findById(child.getId()).orElseThrow();
		assertEquals(EntryType.FREE, reloadedChild.getEntryType());
		assertEquals(EntryStatus.PUBLISHED, reloadedChild.getStatus());
		assertEquals(parent.getId(), reloadedChild.getParentEntry().getId());

		EntryTag reloadedEntryTag = entryTagRepository.findById(entryTag.getId()).orElseThrow();
		assertEquals(TagSource.USER, reloadedEntryTag.getSource());
		assertEquals(TagState.CONFIRMED, reloadedEntryTag.getState());
		assertEquals(TagScope.USER, reloadedEntryTag.getTag().getScope());

		EntrySelfReflection reloadedReflection = entrySelfReflectionRepository.findById(reflection.getId()).orElseThrow();
		assertEquals("내가 느낀 것", reloadedReflection.getContent());
	}

	@Test
	void emotionTagTopicLink_roundTripsWithSharedPrimaryKey() {
		EmotionTagTopic topic = emotionTagTopicRepository.save(
				EmotionTagTopic.create("TEST_TOPIC_" + System.nanoTime(), "테스트 주제", 99));
		Tag emotionTag = tagRepository.save(
				Tag.createSystemTag(TagCategory.EMOTION, "테스트전용감정", "테스트전용감정" + System.nanoTime()));

		EmotionTagTopicLink link = emotionTagTopicLinkRepository.save(EmotionTagTopicLink.assign(emotionTag, topic, 1));

		entityManager.flush();
		entityManager.clear();

		EmotionTagTopicLink reloaded = emotionTagTopicLinkRepository.findById(emotionTag.getId()).orElseThrow();
		assertEquals(emotionTag.getId(), reloaded.getTagId());
		assertEquals(topic.getId(), reloaded.getTopic().getId());
		assertEquals(link.getDisplayOrder(), reloaded.getDisplayOrder());
		assertTrue(emotionTagTopicRepository.findAllByOrderByDisplayOrderAsc().stream()
				.anyMatch(t -> t.getId().equals(topic.getId())));
	}

}
