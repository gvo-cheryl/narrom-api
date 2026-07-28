package com.naroom.api.lifetime;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.lifetime.PeriodCalculator.Period;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodCalculatorTest {

	private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	@Test
	void compute_weeklyReflection_returnsLastCompletedIsoWeek() {
		Period period = PeriodCalculator.compute(AiFeatureType.WEEKLY_REFLECTION, ZONE);

		assertEquals(DayOfWeek.MONDAY, period.start().getDayOfWeek());
		assertEquals(DayOfWeek.SUNDAY, period.end().getDayOfWeek());
		assertEquals(6, ChronoUnit.DAYS.between(period.start(), period.end()));
		assertTrue(period.end().isBefore(LocalDate.now(ZONE)));
	}

	@Test
	void compute_threeDayReflection_returnsRollingWindowEndingToday() {
		LocalDate today = LocalDate.now(ZONE);

		Period period = PeriodCalculator.compute(AiFeatureType.THREE_DAY_REFLECTION, ZONE);

		assertEquals(today, period.end());
		assertEquals(today.minusDays(2), period.start());
	}

	@Test
	void compute_notAPeriodReflectionFeature_throwsIllegalArgumentException() {
		assertThrows(
				IllegalArgumentException.class, () -> PeriodCalculator.compute(AiFeatureType.ENTRY_REFLECTION, ZONE));
	}

}
