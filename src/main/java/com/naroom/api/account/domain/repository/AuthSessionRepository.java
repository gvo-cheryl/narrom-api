package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

	// Access Token(sid)로는 findById로 조회한다. 이 메서드는 /auth/refresh 전용:
	// 재발급 시점엔 Access Token이 만료됐을 수 있어 유일한 단서가 (해시한) Refresh Token뿐이다.
	Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);

}
