package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.ai.AiJobService;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.badge.BadgeAwardService;
import com.naroom.api.badge.domain.entity.BadgeCode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EntryService {

	// CHECK_IN/EXPERIMENT_MISSION/EXPERIMENT_REVIEW/WEEKLY_REFLECTION/SELF_SUMMARY는
	// 각 도메인(Check-in/Experiment/LifeTime) 서비스가 Entry.create()를 직접 호출해서만 만든다.
	// 공개 API로는 사용자가 자유롭게 쓰는 유형만 허용한다.
	private static final Set<EntryType> USER_CREATABLE_TYPES = EnumSet.of(
			EntryType.FREE, EntryType.GRATITUDE, EntryType.EMOTION, EntryType.PROMPT, EntryType.QUOTE_REFLECTION);

	// §7(뱃지 설계) 복귀형 RETURN_AFTER_GAP: 직전 기록과 이번 기록의 recordDate 차이가 이 값 이상이면 공백 뒤 재기록으로 본다.
	private static final long RETURN_GAP_DAYS = 3;

	private static final Logger log = LoggerFactory.getLogger(EntryService.class);

	private final EntryRepository entryRepository;
	private final MemberRepository memberRepository;
	private final QuoteRepository quoteRepository;
	private final AiJobService aiJobService;
	private final BadgeAwardService badgeAwardService;
	private final TransactionTemplate requiresNewTransactionTemplate;

	public EntryService(
			EntryRepository entryRepository,
			MemberRepository memberRepository,
			QuoteRepository quoteRepository,
			AiJobService aiJobService,
			BadgeAwardService badgeAwardService,
			PlatformTransactionManager transactionManager) {
		this.entryRepository = entryRepository;
		this.memberRepository = memberRepository;
		this.quoteRepository = quoteRepository;
		this.aiJobService = aiJobService;
		this.badgeAwardService = badgeAwardService;
		this.requiresNewTransactionTemplate = new TransactionTemplate(transactionManager);
		this.requiresNewTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Transactional
	public EntryResponse createEntry(UUID memberId, EntryCreateRequest request) {
		if (!USER_CREATABLE_TYPES.contains(request.entryType())) {
			throw new BusinessException(RecordErrorCode.ENTRY_TYPE_NOT_USER_CREATABLE);
		}
		Member member = memberRepository.getReferenceById(memberId);
		Entry parentEntry = resolveParentEntry(memberId, request.parentEntryId());
		Quote quote = resolveQuote(request.entryType(), request.quoteId());
		Entry previousEntry = entryRepository.findFirstByMember_IdOrderByRecordDateDescCreatedAtDesc(memberId).orElse(null);

		Entry entry = Entry.create(
				member,
				request.entryType(),
				request.title(),
				request.body(),
				request.recordDate(),
				parentEntry,
				quote,
				request.promptSnapshot());
		Entry savedEntry = entryRepository.save(entry);
		scheduleAiReflectionAfterCommit(memberId, savedEntry);
		awardEntryBadges(memberId, previousEntry, request.recordDate());
		return EntryResponse.from(savedEntry);
	}

	// §7(뱃지 설계) 시도형 FIRST_ENTRY + 복귀형 RETURN_AFTER_GAP. 이 서비스가 만드는 사용자 자유
	// 기록에서만 판정한다 - CHECK_IN/EXPERIMENT_MISSION 등 다른 도메인이 직접 만드는 Entry는 각자의
	// 서비스에서 판정한다(EntryService의 공개 API는 그 유형들을 애초에 받지 않는다).
	private void awardEntryBadges(UUID memberId, Entry previousEntry, LocalDate recordDate) {
		badgeAwardService.award(memberId, BadgeCode.FIRST_ENTRY);
		if (previousEntry != null && ChronoUnit.DAYS.between(previousEntry.getRecordDate(), recordDate) >= RETURN_GAP_DAYS) {
			badgeAwardService.award(memberId, BadgeCode.RETURN_AFTER_GAP);
		}
	}

	// §7.2: 기록 저장 성공과 AI 생성 성공은 서로 다른 결과다 - AI 작업 생성이 실패해도 기록 저장 자체는 실패로
	// 돌리지 않는다. 이 트랜잭션이 커밋된 뒤에만 실행되게 등록한다 - 커밋 전에 바로 호출하면 아직 커밋되지 않은
	// 이 기록을 보지 못해 매번 실패한다. afterCommit() 콜백 안의 코드는 (Spring 문서 기준) 별도 트랜잭션임을
	// 명시하지 않으면 이미 커밋된 원래 트랜잭션의 리소스에 그대로 참여해버려 저장한 내용이 실제로 커밋되지
	// 않는다 - 그래서 AiJobService.createForEntry 자체가 아니라 이 호출 지점에서만 REQUIRES_NEW 트랜잭션
	// 템플릿을 쓴다(createForEntry의 propagation을 REQUIRES_NEW로 바꾸면, 같은 트랜잭션 안에서 회원·기록을
	// 만들고 바로 createForEntry를 호출하는 기존 테스트 다수가 그 회원·기록을 보지 못해 깨진다).
	private void scheduleAiReflectionAfterCommit(UUID memberId, Entry entry) {
		if (!entry.isAiProcessingAllowed()) {
			return;
		}
		UUID entryId = entry.getId();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					requiresNewTransactionTemplate.executeWithoutResult(status -> aiJobService.createForEntry(
							memberId, AiFeatureType.ENTRY_REFLECTION, entryId, "entry-reflection-" + entryId));
				} catch (RuntimeException e) {
					log.warn("failed to create AI job for entry. entryId={} errorType={}", entryId, e.getClass().getSimpleName());
				}
			}
		});
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

	// allow로 되돌려도 이미 지난 기록에 대해 AI 파이프라인을 재실행하지 않는다(5-C, 2026-07-28 결정)
	// - AiJob 생성은 createEntry 시점에만 트리거되고, 이 토글은 장기 회고 입력 포함 여부(§3.3)에만 영향을 준다.
	@Transactional
	public EntryResponse updateAiProcessingAllowed(UUID memberId, UUID entryId, boolean allowed) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		if (allowed) {
			entry.allowAiProcessing();
		} else {
			entry.disallowAiProcessing();
		}
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
