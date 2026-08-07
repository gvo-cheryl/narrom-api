package com.naroom.api.notification;

import com.naroom.api.account.crypto.PushTokenCipher;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.notification.infra.expo.ExpoPushClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@Transactional
@DirtiesContext
class NotificationDispatchServiceTest {

	@Autowired
	private NotificationDispatchService notificationDispatchService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private NotificationPreferenceRepository notificationPreferenceRepository;

	@Autowired
	private PushTokenCipher pushTokenCipher;

	@MockitoBean
	private ExpoPushClient expoPushClient;

	@Test
	void dispatchDue_matchingWindow_sendsAndMarksSent() {
		Member member = memberRepository.save(Member.create("지연"));
		registerDeviceWithToken(member, "ExponentPushToken[test]");
		NotificationPreference preference = createPreference(member, NotificationType.DAILY_QUOTE, true, nowInMemberTimezone(member), null);

		int sent = notificationDispatchService.dispatchDue();

		assertEquals(1, sent);
		verify(expoPushClient).send(eq("ExponentPushToken[test]"), any(), any(), any());
		NotificationPreference reloaded = notificationPreferenceRepository.findById(preference.getId()).orElseThrow();
		assertNotNull(reloaded.getLastSentAt());
	}

	@Test
	void dispatchDue_alreadySentToday_doesNotResend() {
		Member member = memberRepository.save(Member.create("지연"));
		registerDeviceWithToken(member, "ExponentPushToken[test]");
		NotificationPreference preference = createPreference(member, NotificationType.DAILY_QUOTE, true, nowInMemberTimezone(member), null);
		preference.markSent(Instant.now());
		notificationPreferenceRepository.save(preference);

		int sent = notificationDispatchService.dispatchDue();

		assertEquals(0, sent);
		verifyNoInteractions(expoPushClient);
	}

	@Test
	void dispatchDue_disabled_doesNotSend() {
		Member member = memberRepository.save(Member.create("지연"));
		registerDeviceWithToken(member, "ExponentPushToken[test]");
		createPreference(member, NotificationType.DAILY_QUOTE, false, nowInMemberTimezone(member), null);

		int sent = notificationDispatchService.dispatchDue();

		assertEquals(0, sent);
		verifyNoInteractions(expoPushClient);
	}

	@Test
	void dispatchDue_weeklyReflectionWrongDay_doesNotSend() {
		Member member = memberRepository.save(Member.create("지연"));
		registerDeviceWithToken(member, "ExponentPushToken[test]");
		int today = ZonedDateTime.now(ZoneId.of(member.getTimezone())).getDayOfWeek().getValue();
		short wrongDay = (short) (today == 7 ? 1 : today + 1);
		createPreference(member, NotificationType.WEEKLY_REFLECTION, true, nowInMemberTimezone(member), wrongDay);

		int sent = notificationDispatchService.dispatchDue();

		assertEquals(0, sent);
		verifyNoInteractions(expoPushClient);
	}

	private void registerDeviceWithToken(Member member, String rawToken) {
		DeviceInstallation device = deviceInstallationRepository.save(DeviceInstallation.register(member, "device-1", "IOS", "1.0.0"));
		device.updatePushToken(pushTokenCipher.encrypt(rawToken));
	}

	private NotificationPreference createPreference(
			Member member, NotificationType type, boolean enabled, LocalTime localTime, Short dayOfWeek) {
		NotificationPreference preference = NotificationPreference.createDisabled(member, type);
		preference.update(enabled, localTime, dayOfWeek);
		return notificationPreferenceRepository.save(preference);
	}

	private LocalTime nowInMemberTimezone(Member member) {
		return ZonedDateTime.now(ZoneId.of(member.getTimezone())).toLocalTime();
	}

}
