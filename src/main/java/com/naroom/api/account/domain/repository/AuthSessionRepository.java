package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

	// Access Token(sid)로는 findById로 조회한다. 이 메서드는 /auth/refresh 전용:
	// 재발급 시점엔 Access Token이 만료됐을 수 있어 유일한 단서가 (해시한) Refresh Token뿐이다.
	Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

	// 탈퇴 요청 시 활성 세션을 모두 revoke하는 데 쓴다(Account Deletion Rules: 즉시 접근 차단).
	List<AuthSession> findByMember_IdAndRevokedAtIsNull(UUID memberId);

	// 기기가 다른 회원 소유로 재할당될 때, 옛 회원이 이 기기로 발급받았던 활성 세션을 모두 revoke하는 데 쓴다.
	List<AuthSession> findByDeviceInstallation_IdAndRevokedAtIsNull(UUID deviceInstallationId);

	// WITHDRAWAL/DEVICE_REASSIGNED 등 여러 호출부(AccountWithdrawalService,
	// DeviceInstallationService)에서 반복되는 "조회 후 일괄 revoke" 패턴을 하나로 모은다. 세션은 이미
	// 관리 상태(managed)로 조회된 것을 넘겨받는다는 전제라 별도 save() 없이 dirty checking으로 반영된다.
	default void revokeAll(List<AuthSession> sessions, String reason) {
		sessions.forEach(session -> session.revoke(reason));
	}

}
