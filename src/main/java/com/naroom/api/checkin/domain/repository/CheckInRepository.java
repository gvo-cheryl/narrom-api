package com.naroom.api.checkin.domain.repository;

import com.naroom.api.checkin.domain.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

	Optional<CheckIn> findByMember_IdAndCheckInDate(UUID memberId, LocalDate checkInDate);

	List<CheckIn> findByMember_IdAndCheckInDateBetween(UUID memberId, LocalDate start, LocalDate end);

	// 작은 실험 §14 추천 로직: 최근 체크인에서 에너지 낮음이 반복되는지 볼 때 쓴다.
	List<CheckIn> findTop5ByMember_IdOrderByCheckInDateDesc(UUID memberId);

}
