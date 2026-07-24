package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntrySelfReflectionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class EntrySelfReflectionServiceTest {

	@Autowired
	private EntrySelfReflectionService entrySelfReflectionService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void createReflection_multiplePerEntry_areAllStored() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "첫 번째 회고");
		entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "두 번째 회고");

		List<EntrySelfReflectionResponse> reflections = entrySelfReflectionService.listReflections(member.getId(), entry.getId());
		assertEquals(2, reflections.size());
	}

	@Test
	void updateReflection_updatesContent() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		EntrySelfReflectionResponse created =
				entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "원본");

		EntrySelfReflectionResponse updated =
				entrySelfReflectionService.updateReflection(member.getId(), entry.getId(), created.id(), "수정본");

		assertEquals("수정본", updated.content());
	}

	@Test
	void updateReflection_notBelongingToEntry_throwsSelfReflectionNotFound() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entrySelfReflectionService.updateReflection(member.getId(), entry.getId(), UUID.randomUUID(), "내용"));
		assertEquals(RecordErrorCode.SELF_REFLECTION_NOT_FOUND, exception.errorCode());
	}

}
