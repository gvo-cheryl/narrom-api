package com.naroom.api.preview.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 회원 JWT·관리자 세션과 완전히 분리된 미리보기 전용 인증 principal. 역할 기반 인가가 필요 없어
// authorities는 항상 비워 둔다 - preview token을 가진 요청은 이 필터체인 자체가 이미 스코프를 제한한다.
public class PreviewAuthentication extends AbstractAuthenticationToken {

	private final UUID previewSessionId;
	private final Map<String, UUID> selectedContentVersions;
	private final String scenarioKey;

	public PreviewAuthentication(UUID previewSessionId, Map<String, UUID> selectedContentVersions, String scenarioKey) {
		super(List.of());
		this.previewSessionId = previewSessionId;
		this.selectedContentVersions = selectedContentVersions;
		this.scenarioKey = scenarioKey;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return previewSessionId;
	}

	public UUID getPreviewSessionId() {
		return previewSessionId;
	}

	public Map<String, UUID> getSelectedContentVersions() {
		return selectedContentVersions;
	}

	public String getScenarioKey() {
		return scenarioKey;
	}

}
