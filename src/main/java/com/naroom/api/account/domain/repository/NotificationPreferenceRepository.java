package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

	// (member_id, notification_type) unique 제약과 맞춘 upsert 조회용.
	Optional<NotificationPreference> findByMemberIdAndNotificationType(UUID memberId, NotificationType notificationType);

	// 발송 스케줄러가 매 tick마다 후보를 훑는 데 쓴다.
	List<NotificationPreference> findByEnabledTrue();

}
