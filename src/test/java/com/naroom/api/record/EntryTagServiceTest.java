package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagState;
import com.naroom.api.record.domain.error.RecordErrorCode;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class EntryTagServiceTest {

	@Autowired
	private EntryTagService entryTagService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private EntryTagRepository entryTagRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void attachUserTag_calledTwice_doesNotDuplicate() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag tag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.CUSTOM, "태그", "태그" + System.nanoTime()));

		entryTagService.attachUserTag(member.getId(), entry.getId(), tag.getId());
		entryTagService.attachUserTag(member.getId(), entry.getId(), tag.getId());

		assertEquals(1, entryTagRepository.findByEntry_Id(entry.getId()).size());
	}

	@Test
	void confirmTag_updatesStateToConfirmed() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag tag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.CUSTOM, "태그", "태그" + System.nanoTime()));
		EntryTagResponse attached = entryTagService.attachUserTag(member.getId(), entry.getId(), tag.getId());

		EntryTagResponse confirmed = entryTagService.confirmTag(member.getId(), entry.getId(), attached.id());

		assertEquals(TagState.CONFIRMED, confirmed.state());
	}

	@Test
	void rejectTag_updatesStateToRejected() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		Tag tag = tagRepository.save(
				Tag.createUserTag(member, TagCategory.CUSTOM, "태그", "태그" + System.nanoTime()));
		EntryTagResponse attached = entryTagService.attachUserTag(member.getId(), entry.getId(), tag.getId());

		EntryTagResponse rejected = entryTagService.rejectTag(member.getId(), entry.getId(), attached.id());

		assertEquals(TagState.REJECTED, rejected.state());
	}

	@Test
	void confirmTag_notBelongingToEntry_throwsEntryTagNotFound() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryTagService.confirmTag(member.getId(), entry.getId(), UUID.randomUUID()));
		assertEquals(RecordErrorCode.ENTRY_TAG_NOT_FOUND, exception.errorCode());
	}

}
