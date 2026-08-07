package com.naroom.api.notification;

import com.naroom.api.account.crypto.PushTokenCipher;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.notification.config.NotificationDispatchProperties;
import com.naroom.api.notification.infra.expo.ExpoPushClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

// §2 DEC-02(발송 방식): 이벤트가 아니라 주기적 배치로 회원 timezone 기준 local_time/day_of_week가
// 지금 이 tick 구간에 들어오는 알림 설정을 찾아 발송한다. 하루 1회 제한은 last_sent_at의 회원 시간대
// 기준 날짜로 판단한다(그날 이미 보냈으면 dayOfWeek/시각 조건을 다시 만족해도 건너뜀).
@Service
@Transactional(readOnly = true)
public class NotificationDispatchService {

	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final DeviceInstallationRepository deviceInstallationRepository;
	private final PushTokenCipher pushTokenCipher;
	private final ExpoPushClient expoPushClient;
	private final NotificationDispatchProperties properties;

	public NotificationDispatchService(
			NotificationPreferenceRepository notificationPreferenceRepository,
			DeviceInstallationRepository deviceInstallationRepository,
			PushTokenCipher pushTokenCipher,
			ExpoPushClient expoPushClient,
			NotificationDispatchProperties properties) {
		this.notificationPreferenceRepository = notificationPreferenceRepository;
		this.deviceInstallationRepository = deviceInstallationRepository;
		this.pushTokenCipher = pushTokenCipher;
		this.expoPushClient = expoPushClient;
		this.properties = properties;
	}

	@Transactional
	public int dispatchDue() {
		int sentCount = 0;
		for (NotificationPreference preference : notificationPreferenceRepository.findByEnabledTrue()) {
			if (!isDue(preference)) {
				continue;
			}
			sendToAllDevices(preference);
			preference.markSent(Instant.now());
			sentCount++;
		}
		return sentCount;
	}

	private boolean isDue(NotificationPreference preference) {
		if (preference.getLocalTime() == null) {
			return false;
		}
		Member member = preference.getMember();
		ZonedDateTime nowLocal = ZonedDateTime.now(ZoneId.of(member.getTimezone()));

		if (preference.getNotificationType() == NotificationType.WEEKLY_REFLECTION
				&& (preference.getDayOfWeek() == null || nowLocal.getDayOfWeek().getValue() != preference.getDayOfWeek())) {
			return false;
		}

		LocalDate today = nowLocal.toLocalDate();
		if (preference.getLastSentAt() != null
				&& preference.getLastSentAt().atZone(nowLocal.getZone()).toLocalDate().equals(today)) {
			return false;
		}

		// TODO: localTime이 자정 부근(예: 23:58)이면 windowEnd가 다음날로 넘어가며 자정을 지나쳐 창을
		// 놓칠 수 있다 - Beta 1에서는 드문 경계값이라 감수하고, 필요해지면 날짜 넘김을 계산에 반영한다.
		LocalTime nowTime = nowLocal.toLocalTime();
		LocalTime windowEnd = preference.getLocalTime().plus(properties.pollInterval());
		return !nowTime.isBefore(preference.getLocalTime()) && nowTime.isBefore(windowEnd);
	}

	private void sendToAllDevices(NotificationPreference preference) {
		NotificationCopy.Content content = NotificationCopy.forType(preference.getNotificationType());
		List<DeviceInstallation> devices = deviceInstallationRepository
				.findByMember_IdAndRevokedAtIsNullAndPushTokenCiphertextIsNotNull(preference.getMember().getId());
		Map<String, String> data = Map.of("notificationType", preference.getNotificationType().name());
		for (DeviceInstallation device : devices) {
			String pushToken = pushTokenCipher.decrypt(device.getPushTokenCiphertext());
			expoPushClient.send(pushToken, content.title(), content.body(), data);
		}
	}

}
