package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.DeviceInstallation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceInstallationRepository extends JpaRepository<DeviceInstallation, UUID> {

	// 로그인/재발급 시 기존 기기 등록 여부 확인 후 갱신할지 새로 만들지 결정하는 데 쓴다.
	Optional<DeviceInstallation> findByInstallationKey(String installationKey);

	// 로그인 시점 조회 전용: 같은 installationKey로 서로 다른 회원이 거의 동시에 로그인해 재할당 여부를
	// 각자 낡은 스냅샷으로 판단하는 lost update를 막기 위해 행을 잠그고 읽는다. 호출자는 반드시
	// @Transactional 안에서 호출해야 잠금이 트랜잭션 종료까지 유지된다.
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DeviceInstallation d where d.installationKey = :installationKey")
	Optional<DeviceInstallation> findByInstallationKeyForUpdate(String installationKey);

	// 발송 스케줄러가 실제로 푸시를 보낼 수 있는(취소되지 않았고 토큰이 있는) 기기만 골라낸다.
	List<DeviceInstallation> findByMember_IdAndRevokedAtIsNullAndPushTokenCiphertextIsNotNull(UUID memberId);

}
