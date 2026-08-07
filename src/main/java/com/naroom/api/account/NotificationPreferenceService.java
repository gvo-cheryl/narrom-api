package com.naroom.api.account;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.entity.NotificationType;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.account.dto.NotificationPreferenceResponse;
import com.naroom.api.account.dto.NotificationPreferenceUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 온보딩 중 설정(OnboardingService.saveNotificationPreferences)과 별개로, 온보딩 이후 설정 화면(M2)에서
// 언제든 조회·변경할 수 있는 API다.
@Service
@Transactional(readOnly = true)
public class NotificationPreferenceService {

	private final MemberRepository memberRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;

	public NotificationPreferenceService(
			MemberRepository memberRepository, NotificationPreferenceRepository notificationPreferenceRepository) {
		this.memberRepository = memberRepository;
		this.notificationPreferenceRepository = notificationPreferenceRepository;
	}

	// 한 번도 설정한 적 없는 유형도 항상 세 가지 유형 모두를 보여준다(비활성 기본값으로 채움).
	public List<NotificationPreferenceResponse> list(UUID memberId) {
		return List.of(NotificationType.values()).stream()
				.map(type -> notificationPreferenceRepository.findByMemberIdAndNotificationType(memberId, type)
						.map(NotificationPreferenceResponse::from)
						.orElseGet(() -> NotificationPreferenceResponse.defaultFor(type)))
				.toList();
	}

	@Transactional
	public NotificationPreferenceResponse update(UUID memberId, NotificationType type, NotificationPreferenceUpdateRequest request) {
		Member member = memberRepository.getReferenceById(memberId);
		NotificationPreference preference = notificationPreferenceRepository
				.findByMemberIdAndNotificationType(memberId, type)
				.orElseGet(() -> NotificationPreference.createDisabled(member, type));
		Short dayOfWeek = request.dayOfWeek() == null ? null : request.dayOfWeek().shortValue();
		preference.update(request.enabled(), request.localTime(), dayOfWeek);
		return NotificationPreferenceResponse.from(notificationPreferenceRepository.save(preference));
	}

}
