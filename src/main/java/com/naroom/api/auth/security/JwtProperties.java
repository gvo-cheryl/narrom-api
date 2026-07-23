package com.naroom.api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 값은 application.yml(naroom.jwt.*)에서 오고, secret은 그 안에서 다시 ${JWT_SECRET} placeholder로 위임된다.
@ConfigurationProperties(prefix = "naroom.jwt")
public record JwtProperties(
		String secret,
		long accessTokenExpiration,
		long refreshTokenExpiration,
		String issuer,
		String audience) {
}
