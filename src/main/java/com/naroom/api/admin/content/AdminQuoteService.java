package com.naroom.api.admin.content;

import com.naroom.api.admin.content.dto.AdminQuoteCreateRequest;
import com.naroom.api.admin.content.dto.AdminQuoteResponse;
import com.naroom.api.admin.content.dto.AdminQuoteUpdateRequest;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import com.naroom.api.content.domain.entity.QuoteTopic;
import com.naroom.api.content.domain.error.ContentErrorCode;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.content.domain.repository.QuoteTopicRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminQuoteService {

	private final QuoteRepository quoteRepository;
	private final QuoteTopicRepository quoteTopicRepository;

	public AdminQuoteService(QuoteRepository quoteRepository, QuoteTopicRepository quoteTopicRepository) {
		this.quoteRepository = quoteRepository;
		this.quoteTopicRepository = quoteTopicRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminQuoteResponse> list() {
		return quoteRepository.findAllByOrderByCodeAscVersionNoDesc().stream()
				.map(AdminQuoteResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminQuoteResponse get(UUID id) {
		return AdminQuoteResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminQuoteResponse create(AdminQuoteCreateRequest request, UUID actingAdminId) {
		if (quoteRepository.existsByCode(request.code())) {
			throw new BusinessException(ContentErrorCode.QUOTE_CODE_ALREADY_EXISTS);
		}
		Quote quote = Quote.create(
				request.code(), 1, request.text(), request.authorName(), request.sourceName(), request.sourceUrl(),
				request.activeFrom(), request.activeUntil(), null, actingAdminId);
		quote.replaceTopics(resolveTopics(request.topicIds()));
		return AdminQuoteResponse.from(quoteRepository.save(quote));
	}

	@Transactional
	public AdminQuoteResponse update(UUID id, AdminQuoteUpdateRequest request) {
		Quote quote = findOrThrow(id);
		requireStatus(quote, QuoteStatus.DRAFT);
		quote.updateDraft(
				request.text(), request.authorName(), request.sourceName(), request.sourceUrl(),
				request.activeFrom(), request.activeUntil());
		quote.replaceTopics(resolveTopics(request.topicIds()));
		return AdminQuoteResponse.from(quote);
	}

	// §19.4 편집 API 버전 규칙: 발행본은 직접 수정하지 않고, 발행본 내용을 시작점으로 하는 새 DRAFT를 만든다.
	@Transactional
	public AdminQuoteResponse createRevision(UUID publishedId, UUID actingAdminId) {
		Quote published = findOrThrow(publishedId);
		requireStatus(published, QuoteStatus.PUBLISHED);
		Quote draft = Quote.create(
				published.getCode(), published.getVersionNo() + 1, published.getText(), published.getAuthorName(),
				published.getSourceName(), published.getSourceUrl(), published.getActiveFrom(),
				published.getActiveUntil(), published.getId(), actingAdminId);
		draft.replaceTopics(Set.copyOf(published.getTopics()));
		return AdminQuoteResponse.from(quoteRepository.save(draft));
	}

	// 같은 code로 이미 PUBLISHED된 버전이 있으면 먼저 ARCHIVED로 내린다(quotes는 code당 PUBLISHED 1개만 허용).
	@Transactional
	public AdminQuoteResponse publish(UUID draftId) {
		Quote draft = findOrThrow(draftId);
		requireStatus(draft, QuoteStatus.DRAFT);
		quoteRepository.findByCodeAndStatus(draft.getCode(), QuoteStatus.PUBLISHED)
				.ifPresent(Quote::archive);
		draft.publish();
		return AdminQuoteResponse.from(draft);
	}

	@Transactional
	public AdminQuoteResponse archive(UUID id) {
		Quote quote = findOrThrow(id);
		requireStatus(quote, QuoteStatus.PUBLISHED);
		quote.archive();
		return AdminQuoteResponse.from(quote);
	}

	private Set<QuoteTopic> resolveTopics(Set<UUID> topicIds) {
		List<QuoteTopic> found = quoteTopicRepository.findAllById(topicIds);
		if (found.size() != topicIds.size()) {
			throw new BusinessException(ContentErrorCode.QUOTE_TOPIC_NOT_FOUND);
		}
		return found.stream().collect(Collectors.toSet());
	}

	private void requireStatus(Quote quote, QuoteStatus expected) {
		if (quote.getStatus() != expected) {
			throw new BusinessException(
					expected == QuoteStatus.DRAFT ? ContentErrorCode.QUOTE_NOT_DRAFT : ContentErrorCode.QUOTE_NOT_PUBLISHED);
		}
	}

	private Quote findOrThrow(UUID id) {
		return quoteRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ContentErrorCode.QUOTE_NOT_FOUND));
	}

}
