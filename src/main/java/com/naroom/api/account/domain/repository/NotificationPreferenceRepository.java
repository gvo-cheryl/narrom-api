package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

	// (member_id, notification_type) unique 제약과 맞춘 upsert 조회용.
	Optional<NotificationPreference> findByMemberIdAndNotificationType(UUID memberId, NotificationType notificationType);

}
