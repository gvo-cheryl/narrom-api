package com.naroom.api.account;

import com.naroom.api.account.domain.entity.ConsentType;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberConsent;
import com.naroom.api.account.domain.entity.NotificationPreference;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.MemberConsentRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.NotificationPreferenceRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.account.dto.OnboardingCompleteRequest;
import com.naroom.api.account.dto.OnboardingCompleteResponse;
import com.naroom.api.auth.AuthSessionService;
import com.naroom.api.auth.NextAction;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// authentication.md 온보딩 완료 처리 순서를 그대로 따른다.
// 문서 버전을 관리하는 별도 엔티티가 아직 없어 현재 허용 버전을 상수로 고정한다(추후 버전 관리 기능이 생기면 대체한다).
@Service
public class OnboardingService {

	private static final String CURRENT_DOCUMENT_VERSION = "1.0";
	private static final Set<ConsentType> REQUIRED_CONSENT_TYPES =
			Set.of(ConsentType.TERMS, ConsentType.PRIVACY, ConsentType.AI_PROCESSING);
	private static final String CONSENT_SOURCE = "ONBOARDING";

	private final MemberRepository memberRepository;
	private final MemberConsentRepository memberConsentRepository;
	private final NotificationPreferenceRepository notificationPreferenceRepository;
	private final AuthSessionService authSessionService;

	public OnboardingService(
			MemberRepository memberRepository,
			MemberConsentRepository memberConsentRepository,
			NotificationPreferenceRepository notificationPreferenceRepository,
			AuthSessionService authSessionService) {
		this.memberRepository = memberRepository;
		this.memberConsentRepository = memberConsentRepository;
		this.notificationPreferenceRepository = notificationPreferenceRepository;
		this.authSessionService = authSessionService;
	}

	@Transactional
	public OnboardingCompleteResponse complete(UUID memberId, OnboardingCompleteRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalStateException("Authenticated member not found: " + memberId));

		authSessionService.requireLoginableStatus(member);

		if (isReplayOfAlreadyCompletedRequest(member, request.version())) {
			return buildResponse(member);
		}
		if (!member.getVersion().equals(request.version())) {
			throw new BusinessException(AccountErrorCode.ACCOUNT_VERSION_CONFLICT);
		}

		requireCurrentDocumentVersions(request.consents());
		requireAllMandatoryConsentsAgreed(request.consents());

		for (OnboardingCompleteRequest.ConsentRequest consent : request.consents()) {
			memberConsentRepository.save(MemberConsent.record(
					member, consent.type(), consent.documentVersion(), consent.agreed(), CONSENT_SOURCE));
		}
		saveNotificationPreferences(member, request.notificationPreferences());

		member.completeOnboarding(request.displayName(), request.timezone(), request.locale());
		// @Version은 flush 시점에야 증가한다. save()만 하고 바로 응답을 만들면 버전이 반영되지 않은 값을
		// 반환하게 되고, 다음 요청의 재시도(멱등) 판정도 어긋난다.
		memberRepository.saveAndFlush(member);

		return buildResponse(member);
	}

	// 네트워크 재시도로 같은 요청이 다시 오는 경우를 위한 멱등 처리: 온보딩이 이미 완료되어 있고
	// 요청 version이 "완료 직전 버전"과 같으면 그 요청이 만든 결과를 다시 반환하고 저장은 하지 않는다.
	private boolean isReplayOfAlreadyCompletedRequest(Member member, Long requestedVersion) {
		return member.getOnboardingCompletedAt() != null
				&& requestedVersion != null
				&& member.getVersion() - 1 == requestedVersion;
	}

	private void requireCurrentDocumentVersions(List<OnboardingCompleteRequest.ConsentRequest> consents) {
		boolean hasUnsupportedVersion = consents.stream()
				.anyMatch(consent -> !CURRENT_DOCUMENT_VERSION.equals(consent.documentVersion()));
		if (hasUnsupportedVersion) {
			throw new BusinessException(AccountErrorCode.ONBOARDING_DOCUMENT_VERSION_INVALID);
		}
	}

	private void requireAllMandatoryConsentsAgreed(List<OnboardingCompleteRequest.ConsentRequest> consents) {
		Set<ConsentType> agreedTypes = consents.stream()
				.filter(OnboardingCompleteRequest.ConsentRequest::agreed)
				.map(OnboardingCompleteRequest.ConsentRequest::type)
				.collect(Collectors.toSet());
		if (!agreedTypes.containsAll(REQUIRED_CONSENT_TYPES)) {
			throw new BusinessException(AccountErrorCode.ONBOARDING_CONSENT_REQUIRED);
		}
	}

	private void saveNotificationPreferences(
			Member member, List<OnboardingCompleteRequest.NotificationPreferenceRequest> preferences) {
		if (preferences == null) {
			return;
		}
		for (OnboardingCompleteRequest.NotificationPreferenceRequest preference : preferences) {
			NotificationPreference notificationPreference = notificationPreferenceRepository
					.findByMemberIdAndNotificationType(member.getId(), preference.type())
					.orElseGet(() -> NotificationPreference.createDisabled(member, preference.type()));
			Short dayOfWeek = preference.dayOfWeek() == null ? null : preference.dayOfWeek().shortValue();
			notificationPreference.update(preference.enabled(), preference.localTime(), dayOfWeek);
			notificationPreferenceRepository.save(notificationPreference);
		}
	}

	private OnboardingCompleteResponse buildResponse(Member member) {
		NextAction nextAction = member.getOnboardingCompletedAt() == null
				? NextAction.COMPLETE_ONBOARDING
				: NextAction.ENTER_APP;
		return new OnboardingCompleteResponse(
				new AccountSummary(
						member.getId(), member.getStatus(), member.getOnboardingCompletedAt(), member.getVersion()),
				nextAction);
	}

}
