package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntryCreateRequest;
import com.naroom.api.record.dto.EntryResponse;
import com.naroom.api.record.dto.EntryUpdateRequest;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class EntryServiceTest {

	@Autowired
	private EntryService entryService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private QuoteRepository quoteRepository;

	@Test
	void createEntry_userCreatableType_createsDraftEntry() {
		Member member = memberRepository.save(Member.create("지연"));

		EntryResponse response = entryService.createEntry(member.getId(), createRequest(EntryType.FREE, null, null));

		assertEquals(EntryStatus.DRAFT, response.status());
		assertEquals(EntryType.FREE, response.entryType());
	}

	@Test
	void createEntry_notUserCreatableType_throwsEntryTypeNotUserCreatable() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.createEntry(member.getId(), createRequest(EntryType.CHECK_IN, null, null)));
		assertEquals(RecordErrorCode.ENTRY_TYPE_NOT_USER_CREATABLE, exception.errorCode());
	}

	@Test
	void createEntry_parentEntryNotOwnedByMember_throwsEntryNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		Entry parent = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "부모", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.createEntry(stranger.getId(), createRequest(EntryType.FREE, parent.getId(), null)));
		assertEquals(RecordErrorCode.ENTRY_NOT_FOUND, exception.errorCode());
	}

	@Test
	void createEntry_quoteReflectionWithoutQuoteId_throwsEntryTypeQuoteMismatch() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.createEntry(member.getId(), createRequest(EntryType.QUOTE_REFLECTION, null, null)));
		assertEquals(RecordErrorCode.ENTRY_TYPE_QUOTE_MISMATCH, exception.errorCode());
	}

	@Test
	void createEntry_freeTypeWithQuoteId_throwsEntryTypeQuoteMismatch() {
		Member member = memberRepository.save(Member.create("지연"));
		Quote quote = quoteRepository.save(Quote.create("문장", null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.createEntry(member.getId(), createRequest(EntryType.FREE, null, quote.getId())));
		assertEquals(RecordErrorCode.ENTRY_TYPE_QUOTE_MISMATCH, exception.errorCode());
	}

	@Test
	void createEntry_quoteReflectionWithQuoteId_linksQuote() {
		Member member = memberRepository.save(Member.create("지연"));
		Quote quote = quoteRepository.save(Quote.create("문장", null, null, null));

		EntryResponse response =
				entryService.createEntry(member.getId(), createRequest(EntryType.QUOTE_REFLECTION, null, quote.getId()));

		assertEquals(quote.getId(), response.quoteId());
	}

	@Test
	void getEntry_notOwnedByMember_throwsEntryNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.getEntry(stranger.getId(), entry.getId()));
		assertEquals(RecordErrorCode.ENTRY_NOT_FOUND, exception.errorCode());
	}

	@Test
	void listEntries_filtersByEntryTypeAndRecordDate() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "A", today, null, null, null));
		entryRepository.save(Entry.create(member, EntryType.GRATITUDE, null, "B", today, null, null, null));
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "C", yesterday, null, null, null));

		List<EntryResponse> byType = entryService.listEntries(member.getId(), EntryType.FREE, null);
		List<EntryResponse> byDate = entryService.listEntries(member.getId(), null, today);
		List<EntryResponse> all = entryService.listEntries(member.getId(), null, null);

		assertEquals(2, byType.size());
		assertEquals(2, byDate.size());
		assertEquals(3, all.size());
	}

	@Test
	void updateEntry_versionMismatch_throwsEntryVersionConflict() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.updateEntry(
						member.getId(), entry.getId(), new EntryUpdateRequest("제목", "수정본문", 99L)));
		assertEquals(RecordErrorCode.ENTRY_VERSION_CONFLICT, exception.errorCode());
	}

	@Test
	void updateEntry_matchingVersion_updatesTitleAndBody() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		EntryResponse response = entryService.updateEntry(
				member.getId(), entry.getId(), new EntryUpdateRequest("새 제목", "새 본문", entry.getVersion()));

		assertEquals("새 제목", response.title());
		assertEquals("새 본문", response.body());
	}

	@Test
	void publishEntry_setsPublishedStatusAndTimestamp() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		EntryResponse response = entryService.publishEntry(member.getId(), entry.getId());

		assertEquals(EntryStatus.PUBLISHED, response.status());
		assertTrue(response.publishedAt() != null);
	}

	@Test
	void deleteEntry_removesEntry() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		entryService.deleteEntry(member.getId(), entry.getId());

		assertTrue(entryRepository.findById(entry.getId()).isEmpty());
	}

	private EntryCreateRequest createRequest(EntryType entryType, UUID parentEntryId, UUID quoteId) {
		return new EntryCreateRequest(entryType, null, "본문", LocalDate.now(), parentEntryId, quoteId, null);
	}

}
