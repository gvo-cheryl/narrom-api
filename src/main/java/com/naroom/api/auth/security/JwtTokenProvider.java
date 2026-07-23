package com.naroom.api.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// authentication.md JWT claim 최소 원칙: sub/sid/iss/aud/iat/exp만 담는다. 이메일·닉네임 등은 넣지 않는다.
@Component
public class JwtTokenProvider {

	private static final String SESSION_ID_CLAIM = "sid";

	private final SecretKey signingKey;
	private final JwtProperties properties;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
		// HS256이라 secret은 최소 256비트(32바이트) 이상이어야 한다. 짧으면 기동 시 WeakKeyException.
		this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String issueAccessToken(UUID memberId, UUID sessionId) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(memberId.toString())
				.claim(SESSION_ID_CLAIM, sessionId.toString())
				.issuer(properties.issuer())
				.audience().add(properties.audience()).and()
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(properties.accessTokenExpiration())))
				.signWith(signingKey)
				.compact();
	}

	public Instant accessTokenExpiresAt(Instant issuedAt) {
		return issuedAt.plusMillis(properties.accessTokenExpiration());
	}

	/**
	 * 서명·만료·iss/aud만 검증한다. 세션(auth_sessions) 상태 검증은 JwtAuthenticationFilter가 별도로 한다
	 * (JWT 자체는 만료 전까지 항상 "유효"하므로, 로그아웃 같은 즉시 폐기는 DB 조회로만 가능하다).
	 */
	public AccessTokenClaims parse(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		if (!properties.issuer().equals(claims.getIssuer())
				|| claims.getAudience() == null
				|| !claims.getAudience().contains(properties.audience())) {
			throw new JwtException("Unexpected issuer or audience");
		}

		UUID memberId = UUID.fromString(claims.getSubject());
		UUID sessionId = UUID.fromString(claims.get(SESSION_ID_CLAIM, String.class));
		return new AccessTokenClaims(memberId, sessionId);
	}

}
