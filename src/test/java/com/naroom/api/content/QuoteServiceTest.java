package com.naroom.api.content;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.content.domain.entity.MemberSavedQuoteId;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import com.naroom.api.content.domain.entity.QuoteTopic;
import com.naroom.api.content.domain.error.ContentErrorCode;
import com.naroom.api.content.domain.repository.MemberSavedQuoteRepository;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.content.domain.repository.QuoteTopicRepository;
import com.naroom.api.content.dto.QuoteResponse;
import com.naroom.api.content.dto.QuoteTopicResponse;
import com.naroom.api.content.dto.SavedQuoteResponse;
import com.naroom.api.global.error.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class QuoteServiceTest {

	@Autowired
	private QuoteService quoteService;

	@Autowired
	private QuoteRepository quoteRepository;

	@Autowired
	private QuoteTopicRepository quoteTopicRepository;

	@Autowired
	private MemberSavedQuoteRepository memberSavedQuoteRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void getTodayQuote_returnsPublishedQuote_reflectingSavedState() {
		Member member = memberRepository.save(Member.create("지연"));

		QuoteResponse first = quoteService.getTodayQuote(member.getId());
		assertFalse(first.saved());

		quoteService.saveQuote(member.getId(), first.id());
		QuoteResponse second = quoteService.getTodayQuote(member.getId());

		assertEquals(first.id(), second.id());
		assertTrue(second.saved());
	}

	@Test
	void getTodayQuote_noPublishedQuotes_throwsQuoteNotFound() {
		archiveAllPublishedQuotes();

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> quoteService.getTodayQuote(UUID.randomUUID()));
		assertEquals(ContentErrorCode.QUOTE_NOT_FOUND, exception.errorCode());
	}

	@Test
	void getQuote_notFound_throwsQuoteNotFound() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> quoteService.getQuote(UUID.randomUUID(), UUID.randomUUID()));
		assertEquals(ContentErrorCode.QUOTE_NOT_FOUND, exception.errorCode());
	}

	@Test
	void getQuote_found_returnsQuoteDetail() {
		Quote quote = quoteRepository.save(Quote.create("상세 조회 테스트 문장", "작가", "출처", null));
		quote.publish();

		QuoteResponse response = quoteService.getQuote(quote.getId(), UUID.randomUUID());

		assertEquals(quote.getId(), response.id());
		assertEquals("상세 조회 테스트 문장", response.text());
		assertFalse(response.saved());
	}

	@Test
	void getActiveTopics_excludesInactiveTopics() {
		QuoteTopic activeTopic = quoteTopicRepository.save(QuoteTopic.create("ACTIVE_" + System.nanoTime(), "활성 주제"));
		QuoteTopic inactiveTopic = quoteTopicRepository.save(QuoteTopic.create("INACTIVE_" + System.nanoTime(), "비활성 주제"));
		entityManager.createQuery("update QuoteTopic t set t.active = false where t.id = :id")
				.setParameter("id", inactiveTopic.getId())
				.executeUpdate();
		entityManager.clear();

		List<QuoteTopicResponse> topics = quoteService.getActiveTopics();

		assertTrue(topics.stream().anyMatch(t -> t.id().equals(activeTopic.getId())));
		assertFalse(topics.stream().anyMatch(t -> t.id().equals(inactiveTopic.getId())));
	}

	@Test
	void getQuotesByTopic_topicNotFound_throwsQuoteTopicNotFound() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> quoteService.getQuotesByTopic(UUID.randomUUID(), UUID.randomUUID()));
		assertEquals(ContentErrorCode.QUOTE_TOPIC_NOT_FOUND, exception.errorCode());
	}

	@Test
	void getQuotesByTopic_returnsOnlyPublishedQuotesLinkedToTopic() {
		QuoteTopic topic = quoteTopicRepository.save(QuoteTopic.create("LINK_" + System.nanoTime(), "연결 주제"));

		Quote publishedLinked = Quote.create("게시+연결", null, null, null);
		publishedLinked.addTopic(topic);
		publishedLinked.publish();
		quoteRepository.save(publishedLinked);

		Quote draftLinked = Quote.create("초안+연결", null, null, null);
		draftLinked.addTopic(topic);
		quoteRepository.save(draftLinked);

		Quote publishedUnlinked = Quote.create("게시+미연결", null, null, null);
		publishedUnlinked.publish();
		quoteRepository.save(publishedUnlinked);

		List<QuoteResponse> result = quoteService.getQuotesByTopic(topic.getId(), UUID.randomUUID());

		assertEquals(1, result.size());
		assertEquals(publishedLinked.getId(), result.get(0).id());
	}

	@Test
	void saveQuote_calledTwice_doesNotDuplicateOrThrow() {
		Member member = memberRepository.save(Member.create("지연"));
		Quote quote = quoteRepository.save(Quote.create("저장 테스트", null, null, null));

		quoteService.saveQuote(member.getId(), quote.getId());
		quoteService.saveQuote(member.getId(), quote.getId());

		assertEquals(1, memberSavedQuoteRepository.findByMember_IdOrderBySavedAtDesc(member.getId()).stream()
				.filter(saved -> saved.getQuote().getId().equals(quote.getId()))
				.count());
	}

	@Test
	void saveQuote_quoteNotFound_throwsQuoteNotFound() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> quoteService.saveQuote(UUID.randomUUID(), UUID.randomUUID()));
		assertEquals(ContentErrorCode.QUOTE_NOT_FOUND, exception.errorCode());
	}

	@Test
	void unsaveQuote_notSaved_doesNotThrow() {
		quoteService.unsaveQuote(UUID.randomUUID(), UUID.randomUUID());
	}

	@Test
	void unsaveQuote_removesExistingSave() {
		Member member = memberRepository.save(Member.create("지연"));
		Quote quote = quoteRepository.save(Quote.create("저장 취소 테스트", null, null, null));
		quoteService.saveQuote(member.getId(), quote.getId());

		quoteService.unsaveQuote(member.getId(), quote.getId());

		assertFalse(memberSavedQuoteRepository.existsById(new MemberSavedQuoteId(member.getId(), quote.getId())));
	}

	@Test
	void getSavedQuotes_returnsSavedQuotesForMember() {
		Member member = memberRepository.save(Member.create("지연"));
		Quote quoteA = quoteRepository.save(Quote.create("저장 목록 A", null, null, null));
		Quote quoteB = quoteRepository.save(Quote.create("저장 목록 B", null, null, null));
		quoteService.saveQuote(member.getId(), quoteA.getId());
		quoteService.saveQuote(member.getId(), quoteB.getId());

		List<SavedQuoteResponse> saved = quoteService.getSavedQuotes(member.getId());

		assertEquals(2, saved.size());
		assertTrue(saved.stream().allMatch(s -> s.quote().saved()));
		assertTrue(saved.stream().anyMatch(s -> s.quote().id().equals(quoteA.getId())));
		assertTrue(saved.stream().anyMatch(s -> s.quote().id().equals(quoteB.getId())));
	}

	private void archiveAllPublishedQuotes() {
		entityManager.createQuery("update Quote q set q.status = :archived where q.status = :published")
				.setParameter("archived", QuoteStatus.ARCHIVED)
				.setParameter("published", QuoteStatus.PUBLISHED)
				.executeUpdate();
		entityManager.clear();
	}

}
