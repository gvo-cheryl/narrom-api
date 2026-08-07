package com.naroom.api.badge;

import com.naroom.api.badge.domain.repository.MemberBadgeRepository;
import com.naroom.api.badge.dto.MemberBadgeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// §6 DEC-02: 획득한 뱃지만 보여주는 조용한 공간이다 - 아직 획득하지 않은 뱃지의 목록이나 진행률은
// 노출하지 않는다(목표 지향적 표현으로 압박이 될 수 있어 Beta 1 범위에서 제외).
@Service
@Transactional(readOnly = true)
public class BadgeQueryService {

	private final MemberBadgeRepository memberBadgeRepository;

	public BadgeQueryService(MemberBadgeRepository memberBadgeRepository) {
		this.memberBadgeRepository = memberBadgeRepository;
	}

	public List<MemberBadgeResponse> listEarned(UUID memberId) {
		return memberBadgeRepository.findByMember_IdOrderByEarnedAtDesc(memberId).stream()
				.map(MemberBadgeResponse::from)
				.toList();
	}

}
