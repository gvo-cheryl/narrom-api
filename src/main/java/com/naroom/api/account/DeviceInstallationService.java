package com.naroom.api.account;

import com.naroom.api.account.crypto.PushTokenCipher;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.dto.DevicePushTokenUpdateRequest;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 로그인 시 기기 등록·재사용·재할당(registerOrReuseDevice)과, 로그인 이후 비동기로 승인되는 푸시 토큰
// 갱신(updatePushToken, §3 DEC-03: 임시 AES/GCM 암호화)을 모두 이 서비스가 담당한다.
@Service
@Transactional(readOnly = true)
public class DeviceInstallationService {

	// AuthSession.revoke()의 revoke_reason 값(자유 varchar) 중 하나 - AccountWithdrawalService의
	// WITHDRAWAL_REVOKE_REASON, AuthController의 LOGOUT_REVOKE_REASON과 같은 성격이다.
	private static final String DEVICE_REASSIGNED_REVOKE_REASON = "DEVICE_REASSIGNED";

	private final DeviceInstallationRepository deviceInstallationRepository;
	private final AuthSessionRepository authSessionRepository;
	private final PushTokenCipher pushTokenCipher;

	public DeviceInstallationService(
			DeviceInstallationRepository deviceInstallationRepository,
			AuthSessionRepository authSessionRepository,
			PushTokenCipher pushTokenCipher) {
		this.deviceInstallationRepository = deviceInstallationRepository;
		this.authSessionRepository = authSessionRepository;
		this.pushTokenCipher = pushTokenCipher;
	}

	// 로그인 시점에 호출한다(authentication.md "기기 재사용과 재할당"). 같은 installationKey로 다른
	// 회원이 로그인하면(기기 공유, 계정 전환) 이 설치를 새 회원 소유로 재할당하고, 옛 회원이 이 기기로
	// 발급받았던 활성 세션을 모두 폐기한다. findByInstallationKeyForUpdate로 행을 잠근 채 판단부터
	// 반영까지 한 트랜잭션에서 끝내, 같은 기기로 거의 동시에 로그인하는 두 요청이 서로의 재할당을
	// 덮어쓰는 lost update를 막는다.
	@Transactional
	public DeviceInstallation registerOrReuseDevice(Member member, String installationKey, String platform, String appVersion) {
		return deviceInstallationRepository.findByInstallationKeyForUpdate(installationKey)
				.map(existing -> reuse(existing, member, appVersion))
				.orElseGet(() -> deviceInstallationRepository.save(
						DeviceInstallation.register(member, installationKey, platform, appVersion)));
	}

	private DeviceInstallation reuse(DeviceInstallation existing, Member member, String appVersion) {
		if (!existing.getMember().getId().equals(member.getId())) {
			authSessionRepository.revokeAll(
					authSessionRepository.findByDeviceInstallation_IdAndRevokedAtIsNull(existing.getId()),
					DEVICE_REASSIGNED_REVOKE_REASON);
			existing.reassignTo(member);
		}
		existing.markSeen(appVersion);
		return existing;
	}

	@Transactional
	public void updatePushToken(UUID memberId, DevicePushTokenUpdateRequest request) {
		DeviceInstallation device = getOwnedDeviceOrThrow(memberId, request.installationKey());
		device.updatePushToken(pushTokenCipher.encrypt(request.pushToken()));
	}

	// 존재 여부를 드러내지 않기 위해 다른 회원의 기기이거나 등록된 적 없는 installationKey 모두 같은
	// DEVICE_INSTALLATION_NOT_FOUND로 응답한다.
	private DeviceInstallation getOwnedDeviceOrThrow(UUID memberId, String installationKey) {
		DeviceInstallation device = deviceInstallationRepository.findByInstallationKey(installationKey)
				.orElseThrow(() -> new BusinessException(AccountErrorCode.DEVICE_INSTALLATION_NOT_FOUND));
		if (!device.getMember().getId().equals(memberId)) {
			throw new BusinessException(AccountErrorCode.DEVICE_INSTALLATION_NOT_FOUND);
		}
		return device;
	}

}
