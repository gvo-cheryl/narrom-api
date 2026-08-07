package com.naroom.api.badge;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.badge.domain.entity.BadgeDefinition;
import com.naroom.api.badge.domain.entity.MemberBadge;
import com.naroom.api.badge.domain.repository.BadgeDefinitionRepository;
import com.naroom.api.badge.domain.repository.MemberBadgeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// 다른 도메인(record/checkin/lifetime/experiment)이 이벤트 발생 시점에 직접 호출하는 진입점이다
// (이 프로젝트는 Spring 이벤트를 쓰지 않고 서비스 간 직접 호출로 도메인을 연결한다).
// award()는 이미 획득한 뱃지에 대해서는 조용히 아무 것도 하지 않는다(DEC-01: 재획득 없음) - 호출하는
// 쪽에서 "처음인지"를 따로 확인할 필요 없이, 조건이 참일 때마다 그냥 호출하면 된다.
@Service
@Transactional(readOnly = true)
public class BadgeAwardService {

	private final MemberRepository memberRepository;
	private final BadgeDefinitionRepository badgeDefinitionRepository;
	private final MemberBadgeRepository memberBadgeRepository;

	public BadgeAwardService(
			MemberRepository memberRepository,
			BadgeDefinitionRepository badgeDefinitionRepository,
			MemberBadgeRepository memberBadgeRepository) {
		this.memberRepository = memberRepository;
		this.badgeDefinitionRepository = badgeDefinitionRepository;
		this.memberBadgeRepository = memberBadgeRepository;
	}

	@Transactional
	public void award(UUID memberId, BadgeCode code) {
		if (memberBadgeRepository.existsByMember_IdAndBadgeDefinition_Code(memberId, code)) {
			return;
		}
		BadgeDefinition definition = badgeDefinitionRepository.findByCode(code)
				.orElseThrow(() -> new IllegalStateException("시드 데이터에 없는 뱃지 코드입니다: " + code));
		Member member = memberRepository.getReferenceById(memberId);
		memberBadgeRepository.save(MemberBadge.award(member, definition, Instant.now()));
	}

}
