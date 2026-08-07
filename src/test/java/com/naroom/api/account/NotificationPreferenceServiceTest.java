package com.naroom.api.account;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.NotificationPreferenceResponse;
import com.naroom.api.account.dto.NotificationPreferenceUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext
class NotificationPreferenceServiceTest {

	@Autowired
	private NotificationPreferenceService notificationPreferenceService;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	void list_noPreviousPreferences_returnsAllTypesDisabledByDefault() {
		Member member = memberRepository.save(Member.create("지연"));

		List<NotificationPreferenceResponse> preferences = notificationPreferenceService.list(member.getId());

		assertEquals(NotificationType.values().length, preferences.size());
		assertTrue(preferences.stream().noneMatch(NotificationPreferenceResponse::enabled));
	}

	@Test
	void update_newType_createsEnabledPreference() {
		Member member = memberRepository.save(Member.create("지연"));

		NotificationPreferenceResponse response = notificationPreferenceService.update(
				member.getId(), NotificationType.DAILY_QUOTE,
				new NotificationPreferenceUpdateRequest(true, null, LocalTime.of(9, 0)));

		assertTrue(response.enabled());
		assertEquals(LocalTime.of(9, 0), response.localTime());
	}

	@Test
	void update_existingType_updatesInPlace() {
		Member member = memberRepository.save(Member.create("지연"));
		notificationPreferenceService.update(
				member.getId(), NotificationType.WEEKLY_REFLECTION,
				new NotificationPreferenceUpdateRequest(true, 1, LocalTime.of(21, 0)));

		NotificationPreferenceResponse updated = notificationPreferenceService.update(
				member.getId(), NotificationType.WEEKLY_REFLECTION, new NotificationPreferenceUpdateRequest(false, null, null));

		assertFalse(updated.enabled());
		List<NotificationPreferenceResponse> preferences = notificationPreferenceService.list(member.getId());
		assertEquals(1, preferences.stream().filter(p -> p.type() == NotificationType.WEEKLY_REFLECTION).count());
	}

}
