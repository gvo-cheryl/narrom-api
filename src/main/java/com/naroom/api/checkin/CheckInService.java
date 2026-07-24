package com.naroom.api.checkin;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.entity.CheckInEmotion;
import com.naroom.api.checkin.domain.error.CheckInErrorCode;
import com.naroom.api.checkin.domain.repository.CheckInEmotionRepository;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.checkin.dto.CheckInResponse;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import com.naroom.api.record.dto.TagResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// 체크인은 항상 CHECK_IN 유형의 Entry와 1:1로 연결된다(entries.id NOT NULL UNIQUE, reference 스키마 기준).
// 이 서비스만 EntryType.CHECK_IN Entry를 만들 수 있다(EntryService의 공개 API는 이 유형을 거부한다).
@Service
@Transactional(readOnly = true)
public class CheckInService {

	private final CheckInRepository checkInRepository;
	private final CheckInEmotionRepository checkInEmotionRepository;
	private final EntryRepository entryRepository;
	private final MemberRepository memberRepository;
	private final TagRepository tagRepository;

	public CheckInService(
			CheckInRepository checkInRepository,
			CheckInEmotionRepository checkInEmotionRepository,
			EntryRepository entryRepository,
			MemberRepository memberRepository,
			TagRepository tagRepository) {
		this.checkInRepository = checkInRepository;
		this.checkInEmotionRepository = checkInEmotionRepository;
		this.entryRepository = entryRepository;
		this.memberRepository = memberRepository;
		this.tagRepository = tagRepository;
	}

	public Optional<CheckInResponse> getTodayCheckIn(UUID memberId) {
		Member member = memberRepository.getReferenceById(memberId);
		return getCheckIn(memberId, today(member));
	}

	public Optional<CheckInResponse> getCheckIn(UUID memberId, LocalDate checkInDate) {
		return checkInRepository.findByMember_IdAndCheckInDate(memberId, checkInDate).map(this::toResponse);
	}

	// 같은 날짜에 다시 요청하면 새 체크인을 만들지 않고 기존 체크인을 수정한다(UNIQUE(member_id, check_in_date)).
	@Transactional
	public CheckInResponse upsertCheckIn(UUID memberId, CheckInUpsertRequest request) {
		Member member = memberRepository.getReferenceById(memberId);
		CheckIn checkIn = checkInRepository.findByMember_IdAndCheckInDate(memberId, request.checkInDate())
				.orElseGet(() -> createCheckIn(member, request.checkInDate()));

		checkIn.update(
				request.emotionIntensity(),
				request.energyLevel(),
				request.memorableEvent(),
				request.gratitudeNote(),
				request.currentNeed(),
				request.freeNote());
		checkInRepository.save(checkIn);

		replaceEmotions(checkIn, request.emotionTagIds());

		return toResponse(checkIn);
	}

	private CheckIn createCheckIn(Member member, LocalDate checkInDate) {
		Entry entry = Entry.create(member, EntryType.CHECK_IN, null, null, checkInDate, null, null, null);
		entry.publish();
		entryRepository.save(entry);
		return checkInRepository.save(CheckIn.create(member, entry, checkInDate));
	}

	private void replaceEmotions(CheckIn checkIn, List<UUID> emotionTagIds) {
		checkInEmotionRepository.deleteByCheckIn_Id(checkIn.getId());
		if (emotionTagIds == null || emotionTagIds.isEmpty()) {
			return;
		}
		for (UUID tagId : emotionTagIds) {
			Tag tag = tagRepository.findById(tagId)
					.orElseThrow(() -> new BusinessException(RecordErrorCode.TAG_NOT_FOUND));
			if (tag.getCategory() != TagCategory.EMOTION) {
				throw new BusinessException(CheckInErrorCode.CHECK_IN_EMOTION_TAG_INVALID);
			}
			checkInEmotionRepository.save(CheckInEmotion.select(checkIn, tag));
		}
	}

	private CheckInResponse toResponse(CheckIn checkIn) {
		List<TagResponse> emotions = checkInEmotionRepository.findByCheckIn_Id(checkIn.getId()).stream()
				.map(checkInEmotion -> TagResponse.from(checkInEmotion.getTag()))
				.collect(Collectors.toList());
		return CheckInResponse.of(checkIn, emotions);
	}

	private LocalDate today(Member member) {
		return ZonedDateTime.now(ZoneId.of(member.getTimezone())).toLocalDate();
	}

}
