package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.badge.BadgeAwardService;
import com.naroom.api.badge.domain.entity.BadgeCode;
import com.naroom.api.lifetime.domain.entity.PersonalSummary;
import com.naroom.api.lifetime.domain.entity.SummaryScope;
import com.naroom.api.lifetime.domain.repository.PersonalSummaryRepository;
import com.naroom.api.lifetime.dto.PersonalSummaryResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 4단계(§L "나의 정리"): 이번 구현은 CURRENT_SELF 범위만 다룬다 - WEEKLY/MONTHLY/QUARTERLY/EXPERIMENT는
// personal_summaries.scope에 이미 값이 있어 나중에 그대로 확장 가능하지만(1단계 설계), Beta 1 화면은
// "지금의 나"뿐이다. 수정은 기존 글을 고치는 게 아니라 이전 것을 보관(archive)하고 새로 쓰는 방식이다 -
// "이전 정리를 과거 관점으로 보관"(personal_summaries.archived_at 설계 의도)해 이력을 남기기 위함이다.
@Service
@Transactional(readOnly = true)
public class PersonalSummaryService {

	private final MemberRepository memberRepository;
	private final EntryRepository entryRepository;
	private final PersonalSummaryRepository personalSummaryRepository;
	private final BadgeAwardService badgeAwardService;

	public PersonalSummaryService(
			MemberRepository memberRepository,
			EntryRepository entryRepository,
			PersonalSummaryRepository personalSummaryRepository,
			BadgeAwardService badgeAwardService) {
		this.memberRepository = memberRepository;
		this.entryRepository = entryRepository;
		this.personalSummaryRepository = personalSummaryRepository;
		this.badgeAwardService = badgeAwardService;
	}

	public Optional<PersonalSummaryResponse> getCurrent(UUID memberId) {
		return findCurrent(memberId).map(PersonalSummaryResponse::from);
	}

	public List<PersonalSummaryResponse> getHistory(UUID memberId) {
		return personalSummaryRepository.findByMember_IdAndScopeOrderByCreatedAtDesc(memberId, SummaryScope.CURRENT_SELF).stream()
				.map(PersonalSummaryResponse::from)
				.toList();
	}

	@Transactional
	public PersonalSummaryResponse updateCurrent(UUID memberId, String content) {
		Member member = memberRepository.getReferenceById(memberId);
		findCurrent(memberId).ifPresent(previous -> previous.archive(Instant.now()));

		Entry entry = entryRepository.save(
				Entry.create(member, EntryType.SELF_SUMMARY, null, content, LocalDate.now(), null, null, null));
		entry.publish();

		PersonalSummary summary =
				personalSummaryRepository.save(PersonalSummary.create(member, entry, SummaryScope.CURRENT_SELF, null, null));
		badgeAwardService.award(memberId, BadgeCode.FIRST_PERSONAL_SUMMARY);
		return PersonalSummaryResponse.from(summary);
	}

	private Optional<PersonalSummary> findCurrent(UUID memberId) {
		return personalSummaryRepository
				.findByMember_IdAndScopeAndArchivedAtIsNullOrderByCreatedAtDesc(memberId, SummaryScope.CURRENT_SELF)
				.stream()
				.findFirst();
	}

}
