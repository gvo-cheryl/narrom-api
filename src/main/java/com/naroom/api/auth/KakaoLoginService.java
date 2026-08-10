package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.IdentityStatus;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.domain.repository.SocialIdentityRepository;
import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.KakaoLoginRequest;
import com.naroom.api.auth.dto.KakaoLoginResponse;
import com.naroom.api.auth.dto.SessionSummary;
import com.naroom.api.auth.kakao.KakaoClient;
import com.naroom.api.auth.kakao.KakaoUserInfoResponse;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

// authentication.md 카카오 로그인 처리 순서를 그대로 따른다.
@Service
public class KakaoLoginService {

	private static final Set<String> SUPPORTED_PLATFORMS = Set.of("IOS", "ANDROID");
	private static final String DEFAULT_DISPLAY_NAME = "나로움";

	private final KakaoClient kakaoClient;
	private final SocialIdentityRepository socialIdentityRepository;
	private final MemberRepository memberRepository;
	private final DeviceInstallationRepository deviceInstallationRepository;
	private final AuthSessionService authSessionService;

	public KakaoLoginService(
			KakaoClient kakaoClient,
			SocialIdentityRepository socialIdentityRepository,
			MemberRepository memberRepository,
			DeviceInstallationRepository deviceInstallationRepository,
			AuthSessionService authSessionService) {
		this.kakaoClient = kakaoClient;
		this.socialIdentityRepository = socialIdentityRepository;
		this.memberRepository = memberRepository;
		this.deviceInstallationRepository = deviceInstallationRepository;
		this.authSessionService = authSessionService;
	}

	@Transactional
	public KakaoLoginResponse login(KakaoLoginRequest request) {
		SocialIdentity socialIdentity = resolveSocialIdentity(request);
		Member member = socialIdentity.getMember();
		authSessionService.requireLoginableStatus(member);
		return completeAuthentication(socialIdentity, request.device());
	}

	// Account Deletion Rules: 로그인만으로 자동 복구되지 않는다. 카카오로 본인 확인은 하되(계정 존재·
	// REVOKED 여부 확인), PENDING_DELETION이 아니면 거부하고, 맞으면 이 호출 자체를 "명시적 복구 확인"
	// 절차로 간주해(login()과 별도 엔드포인트) 즉시 복구하고 정상 세션을 발급한다.
	@Transactional
	public KakaoLoginResponse restore(KakaoLoginRequest request) {
		SocialIdentity socialIdentity = resolveSocialIdentity(request);
		Member member = socialIdentity.getMember();
		if (member.getStatus() != MemberStatus.PENDING_DELETION) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_NOT_PENDING_DELETION);
		}
		member.restore();
		return completeAuthentication(socialIdentity, request.device());
	}

	private SocialIdentity resolveSocialIdentity(KakaoLoginRequest request) {
		validateDevice(request.device());

		KakaoUserInfoResponse kakaoUser = kakaoClient.fetchUserInfo(request.providerAccessToken());
		String providerUserId = String.valueOf(kakaoUser.id());

		SocialIdentity socialIdentity = socialIdentityRepository
				.findByProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId)
				.orElseGet(() -> connectNewMember(kakaoUser, providerUserId));

		if (socialIdentity.getStatus() == IdentityStatus.REVOKED) {
			throw new BusinessException(AuthErrorCode.AUTH_SOCIAL_IDENTITY_REVOKED);
		}
		return socialIdentity;
	}

	private KakaoLoginResponse completeAuthentication(SocialIdentity socialIdentity, KakaoLoginRequest.DeviceInfo device) {
		Member member = socialIdentity.getMember();
		socialIdentity.recordLogin();

		DeviceInstallation deviceInstallation = registerOrUpdateDevice(member, device);
		IssuedTokens tokens = authSessionService.issue(member, deviceInstallation);

		NextAction nextAction = NextAction.forMember(member);

		return new KakaoLoginResponse(
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

	private void validateDevice(KakaoLoginRequest.DeviceInfo device) {
		if (device.installationKey() == null || device.installationKey().isBlank()) {
			throw new BusinessException(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED);
		}
		if (device.platform() == null || !SUPPORTED_PLATFORMS.contains(device.platform())) {
			throw new BusinessException(AuthErrorCode.DEVICE_PLATFORM_UNSUPPORTED);
		}
	}

	private SocialIdentity connectNewMember(KakaoUserInfoResponse kakaoUser, String providerUserId) {
		String displayName = kakaoUser.nickname() != null ? kakaoUser.nickname() : DEFAULT_DISPLAY_NAME;
		Member member = memberRepository.save(Member.create(displayName));
		SocialIdentity socialIdentity = SocialIdentity.connect(
				member,
				SocialProvider.KAKAO,
				providerUserId,
				kakaoUser.email(),
				kakaoUser.emailVerified(),
				kakaoUser.nickname(),
				kakaoUser.profileImageUrl());
		return socialIdentityRepository.save(socialIdentity);
	}

	private DeviceInstallation registerOrUpdateDevice(Member member, KakaoLoginRequest.DeviceInfo device) {
		return deviceInstallationRepository.findByInstallationKey(device.installationKey())
				.map(existing -> {
					existing.markSeen(device.appVersion());
					return existing;
				})
				.orElseGet(() -> deviceInstallationRepository.save(
						DeviceInstallation.register(member, device.installationKey(), device.platform(), device.appVersion())));
	}

}
