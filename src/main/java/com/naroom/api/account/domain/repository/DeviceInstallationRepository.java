package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, UUID> {

	// 로그인/재발급 시 기존 기기 등록 여부 확인 후 갱신할지 새로 만들지 결정하는 데 쓴다.
	Optional<DeviceInstallation> findByInstallationKey(String installationKey);

	// 발송 스케줄러가 실제로 푸시를 보낼 수 있는(취소되지 않았고 토큰이 있는) 기기만 골라낸다.
	List<DeviceInstallation> findByMember_IdAndRevokedAtIsNullAndPushTokenCiphertextIsNotNull(UUID memberId);

}
