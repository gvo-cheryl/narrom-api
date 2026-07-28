package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.CheckInService;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class CalendarServiceTest {

	@Autowired
	private CalendarService calendarService;

	@Autowired
	private EntryRepository entryRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CheckInService checkInService;

	@Test
	void getMonth_marksDaysWithEntryOrCheckIn() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate entryOnlyDay = LocalDate.of(2026, 7, 10);
		LocalDate checkInDay = LocalDate.of(2026, 7, 15);
		LocalDate emptyDay = LocalDate.of(2026, 7, 20);
		entryRepository.save(Entry.create(member, EntryType.FREE, null, "본문", entryOnlyDay, null, null, null));
		// 체크인도 CHECK_IN 유형의 entries 봉투를 함께 만들므로 checkInDay는 hasEntry/hasCheckIn 모두 true다.
		checkInService.upsertCheckIn(member.getId(), new CheckInUpsertRequest(
				checkInDay, (short) 3, (short) 3, null, null, null, null, List.of()));

		List<CalendarDayResponse> days = calendarService.getMonth(member.getId(), 2026, 7);

		Map<LocalDate, CalendarDayResponse> byDate = days.stream()
				.collect(java.util.stream.Collectors.toMap(CalendarDayResponse::date, d -> d));
		assertEquals(31, days.size());
		assertTrue(byDate.get(entryOnlyDay).hasEntry());
		assertFalse(byDate.get(entryOnlyDay).hasCheckIn());
		assertTrue(byDate.get(checkInDay).hasCheckIn());
		assertTrue(byDate.get(checkInDay).hasEntry());
		assertFalse(byDate.get(emptyDay).hasEntry());
		assertFalse(byDate.get(emptyDay).hasCheckIn());
	}

	@Test
	void getMonth_invalidMonth_throwsCalendarMonthInvalid() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class, () -> calendarService.getMonth(member.getId(), 2026, 13));
		assertEquals(RecordErrorCode.CALENDAR_MONTH_INVALID, exception.errorCode());
	}

}
