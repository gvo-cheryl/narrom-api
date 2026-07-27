package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@Autowired
	private AiReflectionRepository aiReflectionRepository;

	@Test
	void createReflection_withAiReflectionId_linksToAiReflection() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiReflection aiReflection = aiReflectionRepository.save(AiReflection.request(entry, 1));
		aiReflection.complete(null, "요약", "그때 원했던 건 무엇이었나요?", null, null, Instant.now());

		EntrySelfReflectionResponse response = entrySelfReflectionService.createReflection(
				member.getId(), entry.getId(), "쉬고 싶었던 것 같아요", aiReflection.getId());

		assertEquals(aiReflection.getId(), response.aiReflectionId());
	}

	@Test
	void createReflection_withoutAiReflectionId_leavesAiReflectionIdNull() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		EntrySelfReflectionResponse response =
				entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "일반 생각 덧붙이기");

		assertNull(response.aiReflectionId());
	}

	@Test
	void createReflection_aiReflectionNotBelongingToEntry_throwsReflectionNotFound() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entryA = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문A", LocalDate.now(), null, null, null));
		Entry entryB = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문B", LocalDate.now(), null, null, null));
		AiReflection aiReflection = aiReflectionRepository.save(AiReflection.request(entryB, 1));
		aiReflection.complete(null, "요약", "질문", null, null, Instant.now());

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entrySelfReflectionService.createReflection(member.getId(), entryA.getId(), "내용", aiReflection.getId()));
		assertEquals(AiErrorCode.REFLECTION_NOT_FOUND, exception.errorCode());
	}

	@Test
	void createReflection_aiReflectionNotCompleted_throwsReflectionNotFound() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));
		AiReflection pendingReflection = aiReflectionRepository.save(AiReflection.request(entry, 1));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entrySelfReflectionService.createReflection(member.getId(), entry.getId(), "내용", pendingReflection.getId()));
		assertEquals(AiErrorCode.REFLECTION_NOT_FOUND, exception.errorCode());
	}

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
