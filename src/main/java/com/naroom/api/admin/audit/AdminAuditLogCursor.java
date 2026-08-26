package com.naroom.api.admin.audit;

import com.naroom.api.global.error.code.CommonErrorCode;
import com.naroom.api.global.error.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

// docs/api/conventions.md 커서 페이지네이션 규칙: nextCursor는 클라이언트가 해석하지 않는 opaque 문자열이다.
// createdAt(내림차순)만으로는 같은 시각에 여러 로그가 생길 수 있어 id를 tie-breaker로 함께 인코딩한다.
record AdminAuditLogCursor(Instant createdAt, UUID id) {

	String encode() {
		String raw = createdAt.toEpochMilli() + ":" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	static AdminAuditLogCursor decode(String cursor) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			String[] parts = raw.split(":", 2);
			return new AdminAuditLogCursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
		} catch (RuntimeException e) {
			throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
		}
	}

}
