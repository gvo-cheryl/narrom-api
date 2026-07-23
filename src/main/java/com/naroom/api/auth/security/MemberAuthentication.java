package com.naroom.api.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.UUID;

// 역할(role)/권한 모델이 아직 없어 authorities는 항상 비어 있다. 인가는 지금 필요하지 않다.
public class MemberAuthentication extends AbstractAuthenticationToken {

	private final UUID memberId;
	private final UUID sessionId;

	public MemberAuthentication(UUID memberId, UUID sessionId) {
		super(List.of());
		this.memberId = memberId;
		this.sessionId = sessionId;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return memberId;
	}

	public UUID getMemberId() {
		return memberId;
	}

	public UUID getSessionId() {
		return sessionId;
	}

}
