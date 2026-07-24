package com.naroom.api.content;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.content.domain.entity.MemberSavedQuote;
import com.naroom.api.content.domain.entity.MemberSavedQuoteId;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import com.naroom.api.content.domain.error.ContentErrorCode;
import com.naroom.api.content.domain.repository.MemberSavedQuoteRepository;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.content.domain.repository.QuoteTopicRepository;
import com.naroom.api.content.dto.QuoteResponse;
import com.naroom.api.content.dto.QuoteTopicResponse;
import com.naroom.api.content.dto.SavedQuoteResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// "오늘의 문장"은 별도 배정 테이블 없이, 게시 중인 문장을 UTC 날짜 기준으로 순환 배정한다(전 사용자 공통 문장).
// 배정 방식이 제품 요구사항과 다르면 이 메서드만 교체하면 된다.
@Service
@Transactional(readOnly = true)
public class QuoteService {

	private final QuoteRepository quoteRepository;
	private final QuoteTopicRepository quoteTopicRepository;
	private final MemberSavedQuoteRepository memberSavedQuoteRepository;
	private final MemberRepository memberRepository;

	public QuoteService(
			QuoteRepository quoteRepository,
			QuoteTopicRepository quoteTopicRepository,
			MemberSavedQuoteRepository memberSavedQuoteRepository,
			MemberRepository memberRepository) {
		this.quoteRepository = quoteRepository;
		this.quoteTopicRepository = quoteTopicRepository;
		this.memberSavedQuoteRepository = memberSavedQuoteRepository;
		this.memberRepository = memberRepository;
	}

	public QuoteResponse getTodayQuote(UUID memberId) {
		List<Quote> published = quoteRepository.findByStatus(QuoteStatus.PUBLISHED);
		if (published.isEmpty()) {
			throw new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND);
		}
		long dayIndex = LocalDate.now(ZoneOffset.UTC).toEpochDay();
		int index = (int) Math.floorMod(dayIndex, published.size());
		Quote quote = published.get(index);
		return QuoteResponse.of(quote, isSavedByMember(quote.getId(), memberId));
	}

	public QuoteResponse getQuote(UUID quoteId, UUID memberId) {
		Quote quote = quoteRepository.findById(quoteId)
				.orElseThrow(() -> new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND));
		return QuoteResponse.of(quote, isSavedByMember(quoteId, memberId));
	}

	public List<QuoteTopicResponse> getActiveTopics() {
		return quoteTopicRepository.findByActiveTrue().stream()
				.map(QuoteTopicResponse::from)
				.collect(Collectors.toList());
	}

	public List<QuoteResponse> getQuotesByTopic(UUID topicId, UUID memberId) {
		if (!quoteTopicRepository.existsById(topicId)) {
			throw new BusinessException(ContentErrorCode.QUOTE_TOPIC_NOT_FOUND);
		}
		return quoteRepository.findByTopicIdAndStatus(topicId, QuoteStatus.PUBLISHED).stream()
				.map(quote -> QuoteResponse.of(quote, isSavedByMember(quote.getId(), memberId)))
				.collect(Collectors.toList());
	}

	// 이미 저장된 문장을 다시 저장해도 에러를 내지 않는다(멱등).
	@Transactional
	public void saveQuote(UUID memberId, UUID quoteId) {
		MemberSavedQuoteId id = new MemberSavedQuoteId(memberId, quoteId);
		if (memberSavedQuoteRepository.existsById(id)) {
			return;
		}
		Member member = memberRepository.getReferenceById(memberId);
		Quote quote = quoteRepository.findById(quoteId)
				.orElseThrow(() -> new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND));
		memberSavedQuoteRepository.save(MemberSavedQuote.save(member, quote));
	}

	// 저장돼 있지 않은 문장을 취소해도 에러를 내지 않는다(멱등).
	// deleteById는 대상이 없으면 EmptyResultDataAccessException을 던지므로 존재 확인 후 삭제한다.
	@Transactional
	public void unsaveQuote(UUID memberId, UUID quoteId) {
		MemberSavedQuoteId id = new MemberSavedQuoteId(memberId, quoteId);
		if (memberSavedQuoteRepository.existsById(id)) {
			memberSavedQuoteRepository.deleteById(id);
		}
	}

	public List<SavedQuoteResponse> getSavedQuotes(UUID memberId) {
		return memberSavedQuoteRepository.findByMember_IdOrderBySavedAtDesc(memberId).stream()
				.map(SavedQuoteResponse::from)
				.collect(Collectors.toList());
	}

	private boolean isSavedByMember(UUID quoteId, UUID memberId) {
		return memberSavedQuoteRepository.existsById(new MemberSavedQuoteId(memberId, quoteId));
	}

}
