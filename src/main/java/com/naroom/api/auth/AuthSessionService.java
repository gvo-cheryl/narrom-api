package com.naroom.api.auth;

import com.naroom.api.account.domain.entity.AuthSession;
import com.naroom.api.account.domain.entity.DeviceInstallation;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.entity.MemberStatus;
import com.naroom.api.account.domain.repository.AuthSessionRepository;
import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.security.JwtProperties;
import com.naroom.api.auth.security.JwtTokenProvider;
import com.naroom.api.auth.security.RefreshTokenGenerator;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 카카오 로그인(오늘 12번)·토큰 재발급(오늘 11번)이 공통으로 쓸 세션 발급·회전 로직.
 * Controller/DTO는 아직 없다 — 이번 단계는 인프라만 준비한다.
 */
@Service
public class AuthSessionService {

	private final AuthSessionRepository authSessionRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenGenerator refreshTokenGenerator;
	private final JwtProperties jwtProperties;

	public AuthSessionService(
			AuthSessionRepository authSessionRepository,
			JwtTokenProvider jwtTokenProvider,
			RefreshTokenGenerator refreshTokenGenerator,
			JwtProperties jwtProperties) {
		this.authSessionRepository = authSessionRepository;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenGenerator = refreshTokenGenerator;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public IssuedTokens issue(Member member, DeviceInstallation deviceInstallation) {
		String rawRefreshToken = refreshTokenGenerator.generate();
		Instant refreshTokenExpiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpiration());

		AuthSession session = authSessionRepository.save(AuthSession.issue(
				member,
				deviceInstallation,
				refreshTokenGenerator.hash(rawRefreshToken),
				refreshTokenExpiresAt));

		return buildIssuedTokens(session, rawRefreshToken);
	}

	/** authentication.md 토큰 재발급 처리 순서: 해시로 세션 식별 → revoked/expires·기기 일치 확인 → 회원 상태 재확인 → 회전. */
	@Transactional
	public IssuedTokens rotate(String rawRefreshToken, String installationKey) {
		AuthSession session = authSessionRepository.findByRefreshTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID));

		if (session.getRevokedAt() != null) {
			throw new BusinessException(AuthErrorCode.AUTH_SESSION_REVOKED);
		}
		if (session.getExpiresAt().isBefore(Instant.now())) {
			throw new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
		}
		if (session.getDeviceInstallation() == null
				|| !session.getDeviceInstallation().getInstallationKey().equals(installationKey)) {
			throw new BusinessException(AuthErrorCode.AUTH_DEVICE_MISMATCH);
		}
		requireLoginableStatus(session.getMember());

		String newRawRefreshToken = refreshTokenGenerator.generate();
		Instant newExpiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpiration());
		session.rotate(refreshTokenGenerator.hash(newRawRefreshToken), newExpiresAt);

		return buildIssuedTokens(session, newRawRefreshToken);
	}

	// 카카오 로그인·재발급이 공통으로 쓰는 회원 상태 확인 (LOCKED·PENDING_DELETION이면 세션을 내주지 않는다).
	public void requireLoginableStatus(Member member) {
		if (member.getStatus() == MemberStatus.LOCKED) {
			throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
		}
		if (member.getStatus() == MemberStatus.PENDING_DELETION) {
			throw new BusinessException(
					AuthErrorCode.ACCOUNT_PENDING_DELETION,
					Map.of("scheduledDeletionAt", member.getScheduledDeletionAt()));
		}
	}

	@Transactional
	public void revoke(AuthSession session, String reason) {
		session.revoke(reason);
	}

	// 로그아웃 전용: Access Token의 sid(JWT claim)로만 세션을 알 수 있으므로 UUID로 조회부터 한다.
	@Transactional
	public void revoke(UUID sessionId, String reason) {
		AuthSession session = authSessionRepository.findById(sessionId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_SESSION_NOT_FOUND));
		session.revoke(reason);
	}

	private IssuedTokens buildIssuedTokens(AuthSession session, String rawRefreshToken) {
		Instant now = Instant.now();
		String accessToken = jwtTokenProvider.issueAccessToken(session.getMember().getId(), session.getId());
		return new IssuedTokens(
				accessToken,
				jwtTokenProvider.accessTokenExpiresAt(now),
				rawRefreshToken,
				session.getExpiresAt(),
				session);
	}

}
