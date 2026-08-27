package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.repository.MemberBadgeRepository;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryStatus;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.RecordContentLimit;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.RecordContentLimitRepository;
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

	@Autowired
	private MemberBadgeRepository memberBadgeRepository;

	@Autowired
	private RecordContentLimitRepository recordContentLimitRepository;

	@Test
	void createEntry_bodyExceedsConfiguredLimit_throwsEntryBodyTooLong() {
		setBodyMaxLength(10);
		Member member = memberRepository.save(Member.create("지연"));
		EntryCreateRequest request = new EntryCreateRequest(
				EntryType.FREE, null, "12345678901", LocalDate.now(), null, null, null);

		BusinessException exception = assertThrows(
				BusinessException.class, () -> entryService.createEntry(member.getId(), request));
		assertEquals(RecordErrorCode.ENTRY_BODY_TOO_LONG, exception.errorCode());
	}

	@Test
	void updateEntry_bodyExceedsConfiguredLimit_throwsEntryBodyTooLong() {
		Member member = memberRepository.save(Member.create("지연"));
		EntryResponse created = entryService.createEntry(member.getId(), createRequest(EntryType.FREE, null, null));
		setBodyMaxLength(10);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.updateEntry(
						member.getId(), created.id(), new EntryUpdateRequest(null, "12345678901", 0L)));
		assertEquals(RecordErrorCode.ENTRY_BODY_TOO_LONG, exception.errorCode());
	}

	private void setBodyMaxLength(int bodyMaxLength) {
		RecordContentLimit limit = recordContentLimitRepository.findById(RecordContentLimit.SINGLETON_ID).orElseThrow();
		limit.update(bodyMaxLength, null);
		recordContentLimitRepository.saveAndFlush(limit);
	}

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
	void updateAiProcessingAllowed_disallow_thenAllow_togglesFlag() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		EntryResponse disallowed = entryService.updateAiProcessingAllowed(member.getId(), entry.getId(), false);
		EntryResponse allowed = entryService.updateAiProcessingAllowed(member.getId(), entry.getId(), true);

		assertEquals(false, disallowed.aiProcessingAllowed());
		assertEquals(true, allowed.aiProcessingAllowed());
	}

	@Test
	void updateAiProcessingAllowed_notOwnedByMember_throwsEntryNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		Entry entry = entryRepository.save(
				Entry.create(owner, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> entryService.updateAiProcessingAllowed(stranger.getId(), entry.getId(), false));
		assertEquals(RecordErrorCode.ENTRY_NOT_FOUND, exception.errorCode());
	}

	@Test
	void deleteEntry_removesEntry() {
		Member member = memberRepository.save(Member.create("지연"));
		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.FREE, null, "본문", LocalDate.now(), null, null, null));

		entryService.deleteEntry(member.getId(), entry.getId());

		assertTrue(entryRepository.findById(entry.getId()).isEmpty());
	}

	@Test
	void createEntry_first_awardsFirstEntryBadge() {
		Member member = memberRepository.save(Member.create("지연"));

		entryService.createEntry(member.getId(), createRequest(EntryType.FREE, null, null));

		assertEquals(1, countBadge(member.getId(), BadgeCode.FIRST_ENTRY));
	}

	// §7(뱃지 설계) 복귀형 RETURN_AFTER_GAP: 직전 기록과 3일 이상 벌어진 뒤 다시 기록하면 획득한다.
	@Test
	void createEntry_afterThreeDayGap_awardsReturnAfterGapBadge() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate firstDate = LocalDate.now().minusDays(4);
		entryService.createEntry(member.getId(), requestWithDate(EntryType.FREE, firstDate));

		entryService.createEntry(member.getId(), requestWithDate(EntryType.FREE, firstDate.plusDays(3)));

		assertEquals(1, countBadge(member.getId(), BadgeCode.RETURN_AFTER_GAP));
	}

	@Test
	void createEntry_withoutGap_doesNotAwardReturnAfterGapBadge() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate firstDate = LocalDate.now().minusDays(2);
		entryService.createEntry(member.getId(), requestWithDate(EntryType.FREE, firstDate));

		entryService.createEntry(member.getId(), requestWithDate(EntryType.FREE, firstDate.plusDays(1)));

		assertEquals(0, countBadge(member.getId(), BadgeCode.RETURN_AFTER_GAP));
	}

	private long countBadge(UUID memberId, BadgeCode code) {
		return memberBadgeRepository.findByMember_IdOrderByEarnedAtDesc(memberId).stream()
				.filter(badge -> badge.getBadgeDefinition().getCode() == code)
				.count();
	}

	private EntryCreateRequest requestWithDate(EntryType entryType, LocalDate recordDate) {
		return new EntryCreateRequest(entryType, null, "본문", recordDate, null, null, null);
	}

	private EntryCreateRequest createRequest(EntryType entryType, UUID parentEntryId, UUID quoteId) {
		return new EntryCreateRequest(entryType, null, "본문", LocalDate.now(), parentEntryId, quoteId, null);
	}

}
