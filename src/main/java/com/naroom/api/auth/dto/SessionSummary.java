package com.naroom.api.auth.dto;

import java.time.Instant;
import java.util.UUID;

// 로그인/재발급 응답이 공통으로 쓰는 session 필드.
public record SessionSummary(UUID id, Instant expiresAt) {
}
