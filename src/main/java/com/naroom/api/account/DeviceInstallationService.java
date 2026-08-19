package com.naroom.api.account;

import com.naroom.api.account.crypto.PushTokenCipher;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.dto.DevicePushTokenUpdateRequest;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 로그인 시 기기 등록(installationKey/platform/appVersion)은 SocialLoginService가 이미 담당한다.
// 이 서비스는 로그인 이후 비동기로 승인되는 푸시 토큰만 별도로 갱신한다(§3 DEC-03: 임시 AES/GCM 암호화).
@Service
@Transactional(readOnly = true)
public class DeviceInstallationService {

	private final DeviceInstallationRepository deviceInstallationRepository;
	private final PushTokenCipher pushTokenCipher;

	public DeviceInstallationService(DeviceInstallationRepository deviceInstallationRepository, PushTokenCipher pushTokenCipher) {
		this.deviceInstallationRepository = deviceInstallationRepository;
		this.pushTokenCipher = pushTokenCipher;
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
