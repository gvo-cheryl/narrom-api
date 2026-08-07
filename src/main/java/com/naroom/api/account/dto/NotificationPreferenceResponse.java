package com.naroom.api.account.dto;

import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;

import java.time.LocalTime;

public record NotificationPreferenceResponse(
		NotificationType type,
		boolean enabled,
		LocalTime localTime,
		Short dayOfWeek) {

	public static NotificationPreferenceResponse from(NotificationPreference preference) {
		return new NotificationPreferenceResponse(
				preference.getNotificationType(), preference.isEnabled(), preference.getLocalTime(), preference.getDayOfWeek());
	}

	// 아직 한 번도 설정한 적 없는 유형은 저장하지 않고 비활성 기본값으로만 보여준다(불필요한 쓰기 방지).
	public static NotificationPreferenceResponse defaultFor(NotificationType type) {
		return new NotificationPreferenceResponse(type, false, null, null);
	}

}
