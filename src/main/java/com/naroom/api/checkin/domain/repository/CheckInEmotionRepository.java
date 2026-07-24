package com.naroom.api.checkin.domain.repository;

import com.naroom.api.checkin.domain.entity.CheckInEmotion;
import com.naroom.api.checkin.domain.entity.CheckInEmotionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CheckInEmotionRepository extends JpaRepository<CheckInEmotion, CheckInEmotionId> {

	List<CheckInEmotion> findByCheckIn_Id(UUID checkInId);

	void deleteByCheckIn_Id(UUID checkInId);

}
