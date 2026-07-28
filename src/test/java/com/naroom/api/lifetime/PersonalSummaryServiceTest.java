package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.lifetime.dto.PersonalSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class PersonalSummaryServiceTest {

	@Autowired
	private PersonalSummaryService personalSummaryService;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void getCurrent_noneYet_returnsEmpty() {
		Member member = memberRepository.save(Member.create("지연"));

		Optional<PersonalSummaryResponse> current = personalSummaryService.getCurrent(member.getId());

		assertTrue(current.isEmpty());
	}

	@Test
	void updateCurrent_firstCall_createsUnarchivedSummary() {
		Member member = memberRepository.save(Member.create("지연"));

		PersonalSummaryResponse created = personalSummaryService.updateCurrent(member.getId(), "요즘의 나는 조금 지쳐 있다");

		assertEquals("요즘의 나는 조금 지쳐 있다", created.content());
		assertFalse(created.archived());
		Optional<PersonalSummaryResponse> current = personalSummaryService.getCurrent(member.getId());
		assertEquals(created.id(), current.orElseThrow().id());
	}

	@Test
	void updateCurrent_calledTwice_archivesPreviousAndCreatesNew() {
		Member member = memberRepository.save(Member.create("지연"));
		PersonalSummaryResponse first = personalSummaryService.updateCurrent(member.getId(), "처음 쓴 정리");

		PersonalSummaryResponse second = personalSummaryService.updateCurrent(member.getId(), "다시 쓴 정리");

		assertFalse(second.archived());
		assertEquals("다시 쓴 정리", personalSummaryService.getCurrent(member.getId()).orElseThrow().content());
		List<PersonalSummaryResponse> history = personalSummaryService.getHistory(member.getId());
		assertEquals(2, history.size());
		assertTrue(history.stream().anyMatch(entry -> entry.id().equals(first.id()) && entry.archived()));
	}

}
