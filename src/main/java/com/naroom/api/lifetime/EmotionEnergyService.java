package com.naroom.api.lifetime;

import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.lifetime.domain.error.LifetimeErrorCode;
import com.naroom.api.lifetime.dto.EmotionEnergyPointResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 5단계(감정·에너지 흐름): 순위·점수화 금지 원칙에 따라 회원 자신의 체크인 수치를 그대로 보여줄 뿐,
// 평균·추세 점수 등 가공된 지표는 만들지 않는다(해석은 클라이언트/사용자 몫).
@Service
@Transactional(readOnly = true)
public class EmotionEnergyService {

	private static final Set<Integer> ALLOWED_RANGE_DAYS = Set.of(7, 14, 30);

	private final CheckInRepository checkInRepository;

	public EmotionEnergyService(CheckInRepository checkInRepository) {
		this.checkInRepository = checkInRepository;
	}

	public List<EmotionEnergyPointResponse> getTrend(UUID memberId, int rangeDays) {
		if (!ALLOWED_RANGE_DAYS.contains(rangeDays)) {
			throw new BusinessException(LifetimeErrorCode.ANALYTICS_RANGE_INVALID);
		}
		LocalDate end = LocalDate.now();
		LocalDate start = end.minusDays(rangeDays - 1L);
		return checkInRepository.findByMember_IdAndCheckInDateBetween(memberId, start, end).stream()
				.sorted(Comparator.comparing(CheckIn::getCheckInDate))
				.map(checkIn -> new EmotionEnergyPointResponse(
						checkIn.getCheckInDate(), checkIn.getEmotionIntensity(), checkIn.getEnergyLevel()))
				.toList();
	}

}
