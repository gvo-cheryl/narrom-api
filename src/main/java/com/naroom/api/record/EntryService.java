package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.error.ContentErrorCode;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntryCreateRequest;
import com.naroom.api.record.dto.EntryResponse;
import com.naroom.api.record.dto.EntryUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EntryService {

	private final EntryRepository entryRepository;
	private final MemberRepository memberRepository;
	private final QuoteRepository quoteRepository;

	public EntryService(EntryRepository entryRepository, MemberRepository memberRepository, QuoteRepository quoteRepository) {
		this.entryRepository = entryRepository;
		this.memberRepository = memberRepository;
		this.quoteRepository = quoteRepository;
	}

	@Transactional
	public EntryResponse createEntry(UUID memberId, EntryCreateRequest request) {
		Member member = memberRepository.getReferenceById(memberId);
		Entry parentEntry = resolveParentEntry(memberId, request.parentEntryId());
		Quote quote = resolveQuote(request.entryType(), request.quoteId());

		Entry entry = Entry.create(
				member,
				request.entryType(),
				request.title(),
				request.body(),
				request.recordDate(),
				parentEntry,
				quote,
				request.promptSnapshot());
		return EntryResponse.from(entryRepository.save(entry));
	}

	public EntryResponse getEntry(UUID memberId, UUID entryId) {
		return EntryResponse.from(getOwnedEntryOrThrow(memberId, entryId));
	}

	public List<EntryResponse> listEntries(UUID memberId, EntryType entryType, LocalDate recordDate) {
		List<Entry> entries;
		if (recordDate != null) {
			entries = entryRepository.findByMember_IdAndRecordDateOrderByCreatedAtDesc(memberId, recordDate);
		} else if (entryType != null) {
			entries = entryRepository.findByMember_IdAndEntryTypeOrderByRecordDateDescCreatedAtDesc(memberId, entryType);
		} else {
			entries = entryRepository.findByMember_IdOrderByRecordDateDescCreatedAtDesc(memberId);
		}
		return entries.stream().map(EntryResponse::from).collect(Collectors.toList());
	}

	@Transactional
	public EntryResponse updateEntry(UUID memberId, UUID entryId, EntryUpdateRequest request) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		if (!entry.getVersion().equals(request.version())) {
			throw new BusinessException(RecordErrorCode.ENTRY_VERSION_CONFLICT);
		}
		entry.update(request.title(), request.body());
		return EntryResponse.from(entryRepository.saveAndFlush(entry));
	}

	@Transactional
	public EntryResponse publishEntry(UUID memberId, UUID entryId) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		entry.publish();
		return EntryResponse.from(entryRepository.saveAndFlush(entry));
	}

	@Transactional
	public void deleteEntry(UUID memberId, UUID entryId) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		entryRepository.delete(entry);
	}

	private Entry getOwnedEntryOrThrow(UUID memberId, UUID entryId) {
		return entryRepository.findByIdAndMember_Id(entryId, memberId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
	}

	// 부모 기록은 반드시 같은 회원 소유여야 한다(reference 스키마 주석: DB CHECK로 못 잡는 교차 행 규칙).
	private Entry resolveParentEntry(UUID memberId, UUID parentEntryId) {
		if (parentEntryId == null) {
			return null;
		}
		return getOwnedEntryOrThrow(memberId, parentEntryId);
	}

	// QUOTE_REFLECTION 유형만 문장을 연결할 수 있고, 그 외 유형은 문장을 연결할 수 없다.
	private Quote resolveQuote(EntryType entryType, UUID quoteId) {
		boolean isQuoteReflection = entryType == EntryType.QUOTE_REFLECTION;
		if (isQuoteReflection != (quoteId != null)) {
			throw new BusinessException(RecordErrorCode.ENTRY_TYPE_QUOTE_MISMATCH);
		}
		if (quoteId == null) {
			return null;
		}
		return quoteRepository.findById(quoteId)
				.orElseThrow(() -> new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND));
	}

}
