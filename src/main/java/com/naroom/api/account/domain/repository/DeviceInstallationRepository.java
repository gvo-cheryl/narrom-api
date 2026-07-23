package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, UUID> {

	// 로그인/재발급 시 기존 기기 등록 여부 확인 후 갱신할지 새로 만들지 결정하는 데 쓴다.
	Optional<DeviceInstallation> findByInstallationKey(String installationKey);

}
