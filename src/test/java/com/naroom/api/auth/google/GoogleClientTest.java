package com.naroom.api.auth.google;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 실제 Google JWKS 대신 로컬에서 생성한 RSA 키로 서명한 ID Token으로 GoogleClient의 검증 로직만 단위 테스트한다.
class GoogleClientTest {

	private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
	private static final String ISSUER = "https://accounts.google.com";

	@Test
	void validToken_returnsGoogleUserInfo() throws Exception {
		RSAKey signingKey = generateKey();
		GoogleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, "108234567890123456789",
				Instant.now().plusSeconds(3600), "user@example.com", true, "지연", "https://example.com/pic.jpg");

		GoogleUserInfo userInfo = client.verify(token);

		assertEquals("108234567890123456789", userInfo.sub());
		assertEquals("user@example.com", userInfo.email());
		assertEquals(true, userInfo.emailVerified());
		assertEquals("지연", userInfo.name());
		assertEquals("https://example.com/pic.jpg", userInfo.picture());
	}

	@Test
	void wrongAudience_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		GoogleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, "other-client-id", "sub-1",
				Instant.now().plusSeconds(3600), null, false, null, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void wrongIssuer_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		GoogleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, "https://evil.example.com", CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), null, false, null, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void expiredToken_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		GoogleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().minusSeconds(60), null, false, null, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void missingSubject_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		GoogleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, null,
				Instant.now().plusSeconds(3600), null, false, null, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void tamperedSignature_throwsProviderTokenInvalid() throws Exception {
		RSAKey trustedKey = generateKey();
		RSAKey attackerKey = generateKey();
		GoogleClient client = clientVerifying(trustedKey);

		// 검증기가 신뢰하는 키가 아니라 다른 키로 서명한 토큰 - 위조된 서명 상황을 재현한다.
		String token = signedToken(attackerKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), null, false, null, null);

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	private GoogleClient clientVerifying(RSAKey signingKey) throws JOSEException {
		ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
				JWSAlgorithm.RS256, new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()))));
		return new GoogleClient(new GoogleAuthProperties(List.of(CLIENT_ID)), processor);
	}

	private RSAKey generateKey() throws JOSEException {
		return new RSAKeyGenerator(2048).keyID("test-key").generate();
	}

	private String signedToken(
			RSAKey signingKey,
			String issuer,
			String audience,
			String subject,
			Instant expiration,
			String email,
			boolean emailVerified,
			String name,
			String picture) throws JOSEException {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
				.issuer(issuer)
				.audience(audience)
				.expirationTime(Date.from(expiration));
		if (subject != null) {
			claims.subject(subject);
		}
		if (email != null) {
			claims.claim("email", email).claim("email_verified", emailVerified);
		}
		if (name != null) {
			claims.claim("name", name);
		}
		if (picture != null) {
			claims.claim("picture", picture);
		}

		SignedJWT signedJwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
				claims.build());
		signedJwt.sign(new RSASSASigner(signingKey));
		return signedJwt.serialize();
	}

}
