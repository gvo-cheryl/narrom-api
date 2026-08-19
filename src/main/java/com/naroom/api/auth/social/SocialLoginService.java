package com.naroom.api.auth.social;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.IdentityStatus;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.AuthSessionService;
import com.naroom.api.auth.IssuedTokens;
import com.naroom.api.auth.NextAction;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.DeviceInfo;
import com.naroom.api.auth.dto.SessionSummary;
import com.naroom.api.auth.dto.SocialLoginResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// authentication.md 카카오 로그인 처리 순서를 provider 공통으로 일반화한 것.
// 카카오/Google/Apple 각 SocialProviderClient가 자격 증명 검증만 다르게 하고, 이후 흐름(SocialIdentity
// 조회·회원 생성·기기 등록·세션 발급)은 여기서 한 번만 구현한다.
@Service
public class SocialLoginService {

	private static final Set<String> SUPPORTED_PLATFORMS = Set.of("IOS", "ANDROID");
	private static final String DEFAULT_DISPLAY_NAME = "나로움";

	// AuthSession.revoke()의 revoke_reason 값(자유 varchar) 중 하나 - AccountWithdrawalService의
	// WITHDRAWAL_REVOKE_REASON, AuthController의 LOGOUT_REVOKE_REASON과 같은 성격이다.
	private static final String DEVICE_REASSIGNED_REVOKE_REASON = "DEVICE_REASSIGNED";

	private final Map<SocialProvider, SocialProviderClient> clientsByProvider;
	private final SocialIdentityRepository socialIdentityRepository;
	private final MemberRepository memberRepository;
	private final DeviceInstallationRepository deviceInstallationRepository;
	private final AuthSessionRepository authSessionRepository;
	private final AuthSessionService authSessionService;

	public SocialLoginService(
			List<SocialProviderClient> clients,
			SocialIdentityRepository socialIdentityRepository,
			MemberRepository memberRepository,
			DeviceInstallationRepository deviceInstallationRepository,
			AuthSessionRepository authSessionRepository,
			AuthSessionService authSessionService) {
		this.clientsByProvider = clients.stream()
				.collect(Collectors.toUnmodifiableMap(SocialProviderClient::provider, Function.identity()));
		this.socialIdentityRepository = socialIdentityRepository;
		this.memberRepository = memberRepository;
		this.deviceInstallationRepository = deviceInstallationRepository;
		this.authSessionRepository = authSessionRepository;
		this.authSessionService = authSessionService;
	}

	@Transactional
	public SocialLoginResponse login(SocialProvider provider, SocialCredential credential, DeviceInfo device) {
		SocialIdentity socialIdentity = resolveSocialIdentity(provider, credential, device);
		Member member = socialIdentity.getMember();
		authSessionService.requireLoginableStatus(member);
		return completeAuthentication(socialIdentity, device);
	}

	// Account Deletion Rules: 로그인만으로 자동 복구되지 않는다. provider로 본인 확인은 하되(계정 존재·
	// REVOKED 여부 확인), PENDING_DELETION이 아니면 거부하고, 맞으면 이 호출 자체를 "명시적 복구 확인"
	// 절차로 간주해(login()과 별도 진입점) 즉시 복구하고 정상 세션을 발급한다.
	@Transactional
	public SocialLoginResponse restore(SocialProvider provider, SocialCredential credential, DeviceInfo device) {
		SocialIdentity socialIdentity = resolveSocialIdentity(provider, credential, device);
		Member member = socialIdentity.getMember();
		if (member.getStatus() != MemberStatus.PENDING_DELETION) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_PENDING_DELETION);
		}
		member.restore();
		return completeAuthentication(socialIdentity, device);
	}

	private SocialIdentity resolveSocialIdentity(SocialProvider provider, SocialCredential credential, DeviceInfo device) {
		validateDevice(device);

		SocialUserInfo userInfo = clientOf(provider).fetchUserInfo(credential);

		SocialIdentity socialIdentity = socialIdentityRepository
				.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
				.orElseGet(() -> connectNewMember(provider, userInfo));

		if (socialIdentity.getStatus() == IdentityStatus.REVOKED) {
			throw new BusinessException(AuthErrorCode.AUTH_SOCIAL_IDENTITY_REVOKED);
		}
		return socialIdentity;
	}

	private SocialProviderClient clientOf(SocialProvider provider) {
		SocialProviderClient client = clientsByProvider.get(provider);
		if (client == null) {
			throw new IllegalStateException("No SocialProviderClient registered for provider " + provider);
		}
		return client;
	}

	private SocialLoginResponse completeAuthentication(SocialIdentity socialIdentity, DeviceInfo device) {
		Member member = socialIdentity.getMember();
		socialIdentity.recordLogin();

		DeviceInstallation deviceInstallation = registerOrUpdateDevice(member, device);
		IssuedTokens tokens = authSessionService.issue(member, deviceInstallation);

		NextAction nextAction = NextAction.forMember(member);

		return new SocialLoginResponse(
				"Bearer",
				tokens.accessToken(),
				tokens.accessTokenExpiresAt(),
				tokens.refreshToken(),
				tokens.refreshTokenExpiresAt(),
				new SessionSummary(tokens.session().getId(), tokens.session().getExpiresAt()),
				new AccountSummary(
						member.getId(),
						member.getDisplayName(),
						member.getStatus(),
						member.getOnboardingCompletedAt(),
						member.getVersion()),
				nextAction);
	}

	private void validateDevice(DeviceInfo device) {
		if (device.installationKey() == null || device.installationKey().isBlank()) {
			throw new BusinessException(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED);
		}
		if (device.platform() == null || !SUPPORTED_PLATFORMS.contains(device.platform())) {
			throw new BusinessException(AuthErrorCode.DEVICE_PLATFORM_UNSUPPORTED);
		}
	}

	private SocialIdentity connectNewMember(SocialProvider provider, SocialUserInfo userInfo) {
		String displayName = userInfo.profileName() != null ? userInfo.profileName() : DEFAULT_DISPLAY_NAME;
		Member member = memberRepository.save(Member.create(displayName));
		SocialIdentity socialIdentity = SocialIdentity.connect(
				member,
				provider,
				userInfo.providerUserId(),
				userInfo.email(),
				userInfo.emailVerified(),
				userInfo.profileName(),
				userInfo.profileImageUrl());
		return socialIdentityRepository.save(socialIdentity);
	}

	private DeviceInstallation registerOrUpdateDevice(Member member, DeviceInfo device) {
		return deviceInstallationRepository.findByInstallationKey(device.installationKey())
				.map(existing -> reuseDevice(existing, member, device))
				.orElseGet(() -> deviceInstallationRepository.save(
						DeviceInstallation.register(member, device.installationKey(), device.platform(), device.appVersion())));
	}

	// 같은 기기(installationKey)로 다른 회원이 로그인하는 경우(기기 공유, 계정 전환) - 이 설치를 새 회원
	// 소유로 옮기고, 옛 회원이 이 기기로 발급받았던 활성 세션은 모두 폐기한다. 그대로 두면 옛 회원의 세션이
	// 새 회원 소유가 된 기기를 계속 가리키게 되어(§ DeviceInstallationService 소유권 검증과 불일치) 이후
	// 푸시 토큰 갱신 등이 DEVICE_INSTALLATION_NOT_FOUND로 실패한다.
	private DeviceInstallation reuseDevice(DeviceInstallation existing, Member member, DeviceInfo device) {
		if (!existing.getMember().getId().equals(member.getId())) {
			authSessionRepository.findByDeviceInstallation_IdAndRevokedAtIsNull(existing.getId())
					.forEach(session -> session.revoke(DEVICE_REASSIGNED_REVOKE_REASON));
			existing.reassignTo(member);
		}
		existing.markSeen(device.appVersion());
		return existing;
	}

}
