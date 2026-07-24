package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import com.naroom.api.record.domain.repository.TagRepository;
import com.naroom.api.record.dto.EmotionTagTopicResponse;
import com.naroom.api.record.dto.TagResponse;
import com.naroom.api.record.dto.UserTagCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class TagServiceTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void listSystemTags_returnsOnlySystemScopedActiveTags() {
		List<TagResponse> tags = tagService.listSystemTags();

		assertTrue(tags.size() >= 48);
		assertTrue(tags.stream().allMatch(tag -> tag.scope() == TagScope.SYSTEM));
	}

	@Test
	void listUserTags_returnsOnlyTagsOwnedByMember() {
		Member member = memberRepository.save(Member.create("지연"));
		tagService.createUserTag(member.getId(), new UserTagCreateRequest(TagCategory.CUSTOM, "나만의태그"));

		List<TagResponse> tags = tagService.listUserTags(member.getId());

		assertEquals(1, tags.size());
		assertEquals("나만의태그", tags.get(0).name());
		assertEquals(TagScope.USER, tags.get(0).scope());
	}

	@Test
	void createUserTag_calledTwiceWithSameNormalizedName_doesNotDuplicate() {
		Member member = memberRepository.save(Member.create("지연"));

		tagService.createUserTag(member.getId(), new UserTagCreateRequest(TagCategory.CUSTOM, "중복태그"));
		tagService.createUserTag(member.getId(), new UserTagCreateRequest(TagCategory.CUSTOM, "  중복태그  "));

		assertEquals(1, tagRepository.findByOwnerMember_IdAndActiveTrue(member.getId()).size());
	}

	@Test
	void listEmotionTagTopics_returnsAllSixTopicsWithMappedTags() {
		List<EmotionTagTopicResponse> topics = tagService.listEmotionTagTopics();

		assertEquals(6, topics.size());
		assertEquals("COMFORTABLE", topics.get(0).code());
		assertEquals(48, topics.stream().mapToInt(topic -> topic.tags().size()).sum());
		assertTrue(topics.stream()
				.flatMap(topic -> topic.tags().stream())
				.allMatch(tag -> tag.category() == TagCategory.EMOTION));
	}

}
