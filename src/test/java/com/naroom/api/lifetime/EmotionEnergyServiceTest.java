package com.naroom.api.lifetime;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.checkin.CheckInService;
import com.naroom.api.checkin.dto.CheckInUpsertRequest;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.dto.EmotionEnergyPointResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class EmotionEnergyServiceTest {

	@Autowired
	private EmotionEnergyService emotionEnergyService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private CheckInService checkInService;

	@Test
	void getTrend_returnsOnlyDaysWithCheckIns() {
		Member member = memberRepository.save(Member.create("지연"));
		LocalDate today = LocalDate.now();
		checkInService.upsertCheckIn(member.getId(), new CheckInUpsertRequest(
				today.minusDays(1), (short) 3, (short) 4, null, null, null, null, List.of()));
		checkInService.upsertCheckIn(member.getId(), new CheckInUpsertRequest(
				today, (short) 5, (short) 2, null, null, null, null, List.of()));

		List<EmotionEnergyPointResponse> trend = emotionEnergyService.getTrend(member.getId(), 7);

		assertEquals(2, trend.size());
		assertEquals(today.minusDays(1), trend.get(0).date());
		assertEquals(today, trend.get(1).date());
		assertEquals((short) 5, trend.get(1).emotionIntensity());
	}

	@Test
	void getTrend_invalidRange_throwsAnalyticsRangeInvalid() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class, () -> emotionEnergyService.getTrend(member.getId(), 10));
		assertEquals(LifetimeErrorCode.ANALYTICS_RANGE_INVALID, exception.errorCode());
	}

}
