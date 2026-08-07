package com.naroom.api.badge;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.entity.MemberBadge;
import com.naroom.api.badge.domain.repository.MemberBadgeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@DirtiesContext
class BadgeAwardServiceTest {

	@Autowired
	private BadgeAwardService badgeAwardService;

	@Autowired
	private MemberBadgeRepository memberBadgeRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void award_newBadge_createsMemberBadge() {
		Member member = memberRepository.save(Member.create("지연"));

		badgeAwardService.award(member.getId(), BadgeCode.FIRST_ENTRY);

		List<MemberBadge> earned = memberBadgeRepository.findByMember_IdOrderByEarnedAtDesc(member.getId());
		assertEquals(1, earned.size());
		assertEquals(BadgeCode.FIRST_ENTRY, earned.get(0).getBadgeDefinition().getCode());
	}

	// DEC-01: 같은 뱃지는 회원당 한 번만 획득한다 - 판정 조건이 다시 참이 되어 award()가 여러 번
	// 호출돼도 중복 저장하지 않는다.
	@Test
	void award_calledTwice_doesNotDuplicate() {
		Member member = memberRepository.save(Member.create("지연"));

		badgeAwardService.award(member.getId(), BadgeCode.FIRST_CHECKIN);
		badgeAwardService.award(member.getId(), BadgeCode.FIRST_CHECKIN);

		assertEquals(1, memberBadgeRepository.findByMember_IdOrderByEarnedAtDesc(member.getId()).size());
	}

	@Test
	void award_differentCodes_createsSeparateBadges() {
		Member member = memberRepository.save(Member.create("지연"));

		badgeAwardService.award(member.getId(), BadgeCode.FIRST_ENTRY);
		badgeAwardService.award(member.getId(), BadgeCode.FIRST_CHECKIN);

		assertEquals(2, memberBadgeRepository.findByMember_IdOrderByEarnedAtDesc(member.getId()).size());
	}

}
