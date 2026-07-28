package com.naroom.api.lifetime.dto;

import java.time.LocalDate;

// 기록하지 않은 날은 0점이나 실패로 표시하지 않는다(LifeTime 제품 원칙) - hasEntry/hasCheckIn이 모두 false인
// 날짜도 그대로 응답에 포함해 프런트가 "기록 없음"과 "0점"을 구분해서 그리지 않아도 되게 한다.
public record CalendarDayResponse(LocalDate date, boolean hasEntry, boolean hasCheckIn) {
}
