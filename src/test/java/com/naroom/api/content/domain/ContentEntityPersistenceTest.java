package com.naroom.api.content.domain;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.content.domain.entity.MemberSavedQuote;
import com.naroom.api.content.domain.entity.MemberSavedQuoteId;
import com.naroom.api.content.domain.entity.Quote;
import com.naroom.api.content.domain.entity.QuoteStatus;
import com.naroom.api.content.domain.entity.QuoteTopic;
import com.naroom.api.content.domain.repository.MemberSavedQuoteRepository;
import com.naroom.api.content.domain.repository.QuoteRepository;
import com.naroom.api.content.domain.repository.QuoteTopicRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Postgres native enum(quote_status), quote_topic_links 다대다, member_saved_quotes 복합키처럼
 * 스키마 검증만으로는 확인되지 않는 실제 저장/조회 왕복을 검증한다.
 */
@SpringBootTest
@Transactional
@DirtiesContext
class ContentEntityPersistenceTest {

	@Autowired
	private QuoteTopicRepository quoteTopicRepository;

	@Autowired
	private QuoteRepository quoteRepository;

	@Autowired
	private MemberSavedQuoteRepository memberSavedQuoteRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void contentAggregate_roundTripsThroughAllTables() {
		QuoteTopic topic = quoteTopicRepository.save(QuoteTopic.create("TEST_TOPIC_" + System.nanoTime(), "테스트 주제"));

		Quote quote = Quote.create("테스트 문장", "테스트 작가", "테스트 출처", "https://example.com");
		quote.addTopic(topic);
		quote.publish();
		quote = quoteRepository.save(quote);

		Member member = memberRepository.save(Member.create("지연"));
		MemberSavedQuote saved = memberSavedQuoteRepository.save(MemberSavedQuote.save(member, quote));

		entityManager.flush();
		entityManager.clear();

		Quote savedQuote = quoteRepository.findById(quote.getId()).orElseThrow();
		assertEquals(QuoteStatus.PUBLISHED, savedQuote.getStatus());
		assertEquals(1, savedQuote.getTopics().size());
		assertEquals(topic.getId(), savedQuote.getTopics().iterator().next().getId());

		assertTrue(quoteTopicRepository.findByActiveTrue().stream()
				.anyMatch(t -> t.getId().equals(topic.getId())));

		MemberSavedQuoteId savedId = new MemberSavedQuoteId(member.getId(), quote.getId());
		MemberSavedQuote reloadedSaved = memberSavedQuoteRepository.findById(savedId).orElseThrow();
		// Postgres timestamptz는 마이크로초 단위로 반올림해 저장하므로 Instant.now()의 나노초 정밀도와
		// 최대 1마이크로초 차이가 날 수 있다. truncate가 아닌 허용오차 비교로 확인한다.
		assertTrue(Duration.between(saved.getSavedAt(), reloadedSaved.getSavedAt()).abs().toNanos() <= 1_000);
		assertEquals(member.getId(), reloadedSaved.getMember().getId());
	}

}
