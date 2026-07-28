package com.naroom.api.lifetime;

import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.dto.CalendarDayResponse;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Calendar는 별도 도메인·테이블이 아니라 entries/check_ins를 날짜 기준으로 조회하는 기능이다
// (docs/domain/lifetime-calendar.md). 감정 등 대표값 집계는 5단계(감정·에너지 흐름 API) 범위라 여기서는
// 존재 여부만 다룬다.
@Service
@Transactional(readOnly = true)
public class CalendarService {

	private final EntryRepository entryRepository;
	private final CheckInRepository checkInRepository;

	public CalendarService(EntryRepository entryRepository, CheckInRepository checkInRepository) {
		this.entryRepository = entryRepository;
		this.checkInRepository = checkInRepository;
	}

	public List<CalendarDayResponse> getMonth(UUID memberId, int year, int month) {
		if (month < 1 || month > 12) {
			throw new BusinessException(RecordErrorCode.CALENDAR_MONTH_INVALID);
		}
		YearMonth yearMonth = YearMonth.of(year, month);
		LocalDate start = yearMonth.atDay(1);
		LocalDate end = yearMonth.atEndOfMonth();

		Set<LocalDate> entryDates = entryRepository
				.findByMember_IdAndRecordDateBetweenOrderByRecordDateDescCreatedAtDesc(memberId, start, end).stream()
				.map(Entry::getRecordDate)
				.collect(Collectors.toSet());
		Set<LocalDate> checkInDates = checkInRepository.findByMember_IdAndCheckInDateBetween(memberId, start, end).stream()
				.map(CheckIn::getCheckInDate)
				.collect(Collectors.toSet());

		return start.datesUntil(end.plusDays(1))
				.map(date -> new CalendarDayResponse(date, entryDates.contains(date), checkInDates.contains(date)))
				.collect(Collectors.toList());
	}

}
