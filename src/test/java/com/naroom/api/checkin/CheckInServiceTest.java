package com.naroom.api.checkin;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.error.CheckInErrorCode;
import com.naroom.api.checkin.domain.repository.CheckInEmotionRepository;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.checkin.dto.CheckInResponse;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class CheckInServiceTest {

	@Autowired
	private CheckInService checkInService;

	@Autowired
	private CheckInRepository checkInRepository;

	@Autowired
	private CheckInEmotionRepository checkInEmotionRepository;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void getTodayCheckIn_noCheckInYet_returnsEmpty() {
		Member member = memberRepository.save(Member.create("지연"));

		Optional<CheckInResponse> result = checkInService.getTodayCheckIn(member.getId());

		assertTrue(result.isEmpty());
	}

	@Test
	void getTodayCheckIn_usesMemberTimezoneDate() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate memberToday = ZonedDateTime.now(ZoneId.of(member.getTimezone())).toLocalDate();
		CheckInResponse created = checkInService.upsertCheckIn(member.getId(), upsertRequest(memberToday, null));

		Optional<CheckInResponse> today = checkInService.getTodayCheckIn(member.getId());

		assertTrue(today.isPresent());
		assertEquals(created.id(), today.get().id());
	}

	@Test
	void upsertCheckIn_firstCall_createsCheckInWithPublishedCheckInEntry() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate date = LocalDate.now();

		CheckInResponse response = checkInService.upsertCheckIn(member.getId(), upsertRequest(date, null));

		assertEquals(EntryType.CHECK_IN, entryRepository.findById(response.entryId()).orElseThrow().getEntryType());
		assertEquals(1, checkInRepository.findByMember_IdAndCheckInDate(member.getId(), date).stream().count());
	}

	@Test
	void upsertCheckIn_sameDateCalledTwice_updatesExistingRatherThanCreatingNew() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate date = LocalDate.now();

		checkInService.upsertCheckIn(member.getId(), upsertRequest(date, null));
		checkInService.upsertCheckIn(member.getId(), new CheckInUpsertRequest(
				date, (short) 5, (short) 5, "두 번째 기억", "두 번째 감사", "두 번째 필요", "두 번째 메모", List.of()));

		CheckIn stored = checkInRepository.findByMember_IdAndCheckInDate(member.getId(), date).orElseThrow();
		assertEquals("두 번째 기억", stored.getMemorableEvent());
		assertEquals(1, entryRepository.findByMember_IdAndRecordDateOrderByCreatedAtDesc(member.getId(), date).stream()
				.filter(entry -> entry.getEntryType() == EntryType.CHECK_IN)
				.count());
	}

	@Test
	void upsertCheckIn_nonEmotionTag_throwsCheckInEmotionTagInvalid() {
		Member member = memberRepository.save(Member.create("지연"));
		Tag situationTag = tagRepository.save(
				Tag.createSystemTag(TagCategory.SITUATION, "테스트상황", "테스트상황" + System.nanoTime()));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> checkInService.upsertCheckIn(
						member.getId(), upsertRequest(LocalDate.now(), List.of(situationTag.getId()))));
		assertEquals(CheckInErrorCode.CHECK_IN_EMOTION_TAG_INVALID, exception.errorCode());
	}

	@Test
	void upsertCheckIn_replacesEmotionSelectionsOnEachCall() {
		Member member = memberRepository.save(Member.create("지연"));
		Tag tagA = tagRepository.save(Tag.createSystemTag(TagCategory.EMOTION, "감정A", "감정A" + System.nanoTime()));
		Tag tagB = tagRepository.save(Tag.createSystemTag(TagCategory.EMOTION, "감정B", "감정B" + System.nanoTime()));
		LocalDate date = LocalDate.now();

		CheckInResponse first = checkInService.upsertCheckIn(member.getId(), upsertRequest(date, List.of(tagA.getId())));
		checkInService.upsertCheckIn(member.getId(), upsertRequest(date, List.of(tagB.getId())));

		List<TagCategory> remaining = checkInEmotionRepository.findByCheckIn_Id(first.id()).stream()
				.map(selection -> selection.getTag().getCategory())
				.toList();
		assertEquals(1, checkInEmotionRepository.findByCheckIn_Id(first.id()).size());
		assertTrue(checkInEmotionRepository.findByCheckIn_Id(first.id()).stream()
				.anyMatch(selection -> selection.getTag().getId().equals(tagB.getId())));
	}

	@Test
	void getCheckIn_noCheckInForDate_returnsEmpty() {
		Member member = memberRepository.save(Member.create("지연"));

		Optional<CheckInResponse> result = checkInService.getCheckIn(member.getId(), LocalDate.now().minusDays(10));

		assertTrue(result.isEmpty());
	}

	private CheckInUpsertRequest upsertRequest(LocalDate date, List<java.util.UUID> emotionTagIds) {
		return new CheckInUpsertRequest(date, (short) 3, (short) 3, "기억", "감사", "필요", "메모", emotionTagIds);
	}

}
