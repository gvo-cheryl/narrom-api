package com.naroom.api.admin.preview;

import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.entity.PreviewSession;
import com.naroom.api.admin.domain.repository.PreviewSessionRepository;
import com.naroom.api.auth.security.RefreshTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// 회원 AuthSessionService·관리자 AdminSessionService와 같은 원칙(원문 토큰 미저장, 해시만 비교)을
// 미리보기 세션에 적용한다.
@Service
public class PreviewSessionService {

	private final PreviewSessionRepository previewSessionRepository;
	private final RefreshTokenGenerator tokenGenerator;
	private final PreviewSessionProperties properties;
	private final ObjectMapper objectMapper;

	public PreviewSessionService(
			PreviewSessionRepository previewSessionRepository,
			RefreshTokenGenerator tokenGenerator,
			PreviewSessionProperties properties,
			ObjectMapper objectMapper) {
		this.previewSessionRepository = previewSessionRepository;
		this.tokenGenerator = tokenGenerator;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public IssuedPreviewSession issue(AdminUser adminUser, Map<String, UUID> selectedContentVersions, String scenarioKey) {
		String rawToken = tokenGenerator.generate();
		Instant expiresAt = Instant.now().plus(properties.tokenTimeout());
		PreviewSession session = previewSessionRepository.save(PreviewSession.issue(
				adminUser, tokenGenerator.hash(rawToken), writeJson(selectedContentVersions), scenarioKey, expiresAt));
		return new IssuedPreviewSession(rawToken, session);
	}

	@Transactional(readOnly = true)
	public Optional<PreviewSession> validate(String rawToken) {
		return previewSessionRepository.findByTokenHash(tokenGenerator.hash(rawToken))
				.filter(session -> session.isActive(Instant.now()));
	}

	public Map<String, UUID> readSelectedContentVersions(PreviewSession session) {
		return objectMapper.readValue(session.getSelectedContentVersions(), new TypeReference<Map<String, UUID>>() {
		});
	}

	private String writeJson(Map<String, UUID> selectedContentVersions) {
		return objectMapper.writeValueAsString(selectedContentVersions);
	}

}
