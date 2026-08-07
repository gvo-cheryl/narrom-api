package com.naroom.api.account;

import com.naroom.api.account.crypto.PushTokenCipher;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.error.AccountErrorCode;
import com.naroom.api.account.domain.repository.DeviceInstallationRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.DevicePushTokenUpdateRequest;
import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@DirtiesContext
class DeviceInstallationServiceTest {

	@Autowired
	private DeviceInstallationService deviceInstallationService;

	@Autowired
	private DeviceInstallationRepository deviceInstallationRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PushTokenCipher pushTokenCipher;

	@Test
	void updatePushToken_ownedDevice_encryptsAndStores() {
		Member member = memberRepository.save(Member.create("지연"));
		DeviceInstallation device =
				deviceInstallationRepository.save(DeviceInstallation.register(member, "device-1", "IOS", "1.0.0"));

		deviceInstallationService.updatePushToken(
				member.getId(), new DevicePushTokenUpdateRequest("device-1", "ExponentPushToken[abc]"));

		DeviceInstallation reloaded = deviceInstallationRepository.findByInstallationKey("device-1").orElseThrow();
		assertEquals("ExponentPushToken[abc]", pushTokenCipher.decrypt(reloaded.getPushTokenCiphertext()));
	}

	@Test
	void updatePushToken_deviceOfDifferentMember_throwsNotFound() {
		Member owner = memberRepository.save(Member.create("소유자"));
		Member stranger = memberRepository.save(Member.create("타인"));
		deviceInstallationRepository.save(DeviceInstallation.register(owner, "device-1", "IOS", "1.0.0"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> deviceInstallationService.updatePushToken(
						stranger.getId(), new DevicePushTokenUpdateRequest("device-1", "ExponentPushToken[abc]")));
		assertEquals(AccountErrorCode.DEVICE_INSTALLATION_NOT_FOUND, exception.errorCode());
	}

	@Test
	void updatePushToken_unknownInstallationKey_throwsNotFound() {
		Member member = memberRepository.save(Member.create("지연"));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> deviceInstallationService.updatePushToken(
						member.getId(), new DevicePushTokenUpdateRequest("unknown-device", "ExponentPushToken[abc]")));
		assertEquals(AccountErrorCode.DEVICE_INSTALLATION_NOT_FOUND, exception.errorCode());
	}

}
