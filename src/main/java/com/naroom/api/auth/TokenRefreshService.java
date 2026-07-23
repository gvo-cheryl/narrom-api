package com.naroom.api.auth;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.auth.dto.RefreshRequest;
import com.naroom.api.auth.dto.RefreshResponse;
import com.naroom.api.auth.dto.SessionSummary;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;

// authentication.md 토큰 재발급 처리 순서를 따른다. 실제 세션 조회·회전은 AuthSessionService가 담당한다.
@Service
public class TokenRefreshService {

	private final AuthSessionService authSessionService;

	public TokenRefreshService(AuthSessionService authSessionService) {
		this.authSessionService = authSessionService;
	}

	public RefreshResponse refresh(RefreshRequest request) {
		if (request.installationKey() == null || request.installationKey().isBlank()) {
			throw new BusinessException(AuthErrorCode.DEVICE_INSTALLATION_KEY_REQUIRED);
		}

		IssuedTokens tokens = authSessionService.rotate(request.refreshToken(), request.installationKey());

		return new RefreshResponse(
				"Bearer",
				tokens.accessToken(),
				tokens.accessTokenExpiresAt(),
				tokens.refreshToken(),
				tokens.refreshTokenExpiresAt(),
				new SessionSummary(tokens.session().getId(), tokens.session().getExpiresAt()));
	}

}
