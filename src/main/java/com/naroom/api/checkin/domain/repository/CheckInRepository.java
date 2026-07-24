package com.naroom.api.checkin.domain.repository;

import com.naroom.api.checkin.domain.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

	Optional<CheckIn> findByMember_IdAndCheckInDate(UUID memberId, LocalDate checkInDate);

}
