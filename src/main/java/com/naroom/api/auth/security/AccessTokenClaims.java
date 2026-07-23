package com.naroom.api.auth.security;

import java.util.UUID;

public record AccessTokenClaims(UUID memberId, UUID sessionId) {
}
