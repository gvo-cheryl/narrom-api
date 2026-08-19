package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminSession;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminSessionRepository;
import com.naroom.api.auth.security.RefreshTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

// 회원 AuthSessionService와 같은 원칙(원문 토큰 미저장, 해시만 비교)을 관리자 세션에 적용한다.
// RefreshTokenGenerator는 이름과 달리 "고엔트로피 난수 생성 + SHA-256 해시"만 하는 범용 컴포넌트라 그대로 재사용한다.
@Service
public class AdminSessionService {

	private final AdminSessionRepository adminSessionRepository;
	private final RefreshTokenGenerator tokenGenerator;
	private final AdminSessionProperties properties;

	public AdminSessionService(
			AdminSessionRepository adminSessionRepository,
			RefreshTokenGenerator tokenGenerator,
			AdminSessionProperties properties) {
		this.adminSessionRepository = adminSessionRepository;
		this.tokenGenerator = tokenGenerator;
		this.properties = properties;
	}

	@Transactional
	public IssuedAdminSession issue(AdminUser adminUser, String ipHash, String userAgentSummary) {
		String rawToken = tokenGenerator.generate();
		Instant expiresAt = Instant.now().plus(properties.absoluteTimeout());
		AdminSession session = adminSessionRepository.save(
				AdminSession.issue(adminUser, tokenGenerator.hash(rawToken), expiresAt, ipHash, userAgentSummary));
		return new IssuedAdminSession(rawToken, session);
	}

	// idle/absolute timeout과 revoked 여부까지 확인된 세션만 돌려준다. 유효하면 last_used_at도 갱신한다.
	@Transactional
	public Optional<AdminSession> validateAndTouch(String rawToken) {
		String hash = tokenGenerator.hash(rawToken);
		Optional<AdminSession> session = adminSessionRepository.findBySessionTokenHash(hash);
		if (session.isEmpty()) {
			return Optional.empty();
		}
		AdminSession found = session.get();
		if (!found.isActive(Instant.now(), properties.idleTimeout())) {
			return Optional.empty();
		}
		found.markUsed();
		return Optional.of(found);
	}

	@Transactional
	public void revoke(String rawToken) {
		adminSessionRepository.findBySessionTokenHash(tokenGenerator.hash(rawToken))
				.ifPresent(AdminSession::revoke);
	}

}
