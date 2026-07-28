package com.naroom.api.lifetime;

import com.naroom.api.ai.domain.entity.AiFeatureType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

// 3단계 3-A: 회원 시간대 기준으로 기간별 회고의 대상 구간을 계산한다.
// - 주간 회고: 가장 최근에 완전히 끝난 ISO 주(월~일). "오늘"이 어느 요일이든 항상 과거에 이미 끝난 주만
//   가리키게 해 같은 주에 여러 번 요청해도 같은 period_start(월요일)로 수렴한다(§6.1 "주차당 1개").
// - 3일 회고: 달력 고정 블록이 아니라 요청 시점 기준 롤링 윈도우(오늘 포함 최근 3일). 매일 요청할 수 있고,
//   기록이 하나라도 있으면 진행한다(2026-07-28 결정) - 근거가 적을 때의 한계는 AI 응답 자체가 스스로 밝힌다.
public final class PeriodCalculator {

	public record Period(LocalDate start, LocalDate end) {
	}

	public static Period compute(AiFeatureType featureType, ZoneId memberZone) {
		LocalDate today = LocalDate.now(memberZone);
		return switch (featureType) {
			case WEEKLY_REFLECTION -> {
				LocalDate currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
				LocalDate lastCompletedWeekMonday = currentWeekMonday.minusWeeks(1);
				yield new Period(lastCompletedWeekMonday, lastCompletedWeekMonday.plusDays(6));
			}
			case THREE_DAY_REFLECTION -> new Period(today.minusDays(2), today);
			default -> throw new IllegalArgumentException(featureType + "은 기간별 회고 대상이 아닙니다");
		};
	}

	private PeriodCalculator() {
	}

}
