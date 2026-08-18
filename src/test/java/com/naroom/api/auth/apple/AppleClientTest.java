package com.naroom.api.auth.apple;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 실제 Apple JWKS 대신 로컬에서 생성한 RSA 키로 서명한 identity token으로 AppleClient의 검증 로직만 단위 테스트한다.
class AppleClientTest {

	private static final String CLIENT_ID = "com.naroom.app";
	private static final String ISSUER = "https://appleid.apple.com";
	private static final String RAW_NONCE = "test-raw-nonce-value";

	@Test
	void validToken_withMatchingNonce_returnsAppleUserInfo() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, "001234.abcdef1234567890.1234",
				Instant.now().plusSeconds(3600), "user@example.com", true, sha256Hex(RAW_NONCE));

		AppleUserInfo userInfo = client.verify(token, RAW_NONCE);

		assertEquals("001234.abcdef1234567890.1234", userInfo.sub());
		assertEquals("user@example.com", userInfo.email());
		assertEquals(true, userInfo.emailVerified());
	}

	@Test
	void emailVerifiedAsStringClaim_parsesCorrectly() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedTokenWithStringEmailVerified(signingKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), "user@example.com", "true", sha256Hex(RAW_NONCE));

		AppleUserInfo userInfo = client.verify(token, RAW_NONCE);

		assertEquals(true, userInfo.emailVerified());
	}

	@Test
	void nonceMismatch_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), null, false, sha256Hex("some-other-nonce"));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void wrongAudience_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, "com.other.app", "sub-1",
				Instant.now().plusSeconds(3600), null, false, sha256Hex(RAW_NONCE));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void wrongIssuer_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, "https://evil.example.com", CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), null, false, sha256Hex(RAW_NONCE));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void expiredToken_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().minusSeconds(60), null, false, sha256Hex(RAW_NONCE));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void missingSubject_throwsProviderTokenInvalid() throws Exception {
		RSAKey signingKey = generateKey();
		AppleClient client = clientVerifying(signingKey);

		String token = signedToken(signingKey, ISSUER, CLIENT_ID, null,
				Instant.now().plusSeconds(3600), null, false, sha256Hex(RAW_NONCE));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	@Test
	void tamperedSignature_throwsProviderTokenInvalid() throws Exception {
		RSAKey trustedKey = generateKey();
		RSAKey attackerKey = generateKey();
		AppleClient client = clientVerifying(trustedKey);

		String token = signedToken(attackerKey, ISSUER, CLIENT_ID, "sub-1",
				Instant.now().plusSeconds(3600), null, false, sha256Hex(RAW_NONCE));

		BusinessException exception = assertThrows(BusinessException.class, () -> client.verify(token, RAW_NONCE));
		assertEquals(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID, exception.errorCode());
	}

	private AppleClient clientVerifying(RSAKey signingKey) throws JOSEException {
		ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		processor.setJWSKeySelector(new JWSVerificationKeySelector<>(
				JWSAlgorithm.RS256, new ImmutableJWKSet<>(new JWKSet(signingKey.toPublicJWK()))));
		return new AppleClient(new AppleAuthProperties(List.of(CLIENT_ID)), processor);
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
			String nonce) throws JOSEException {
		JWTClaimsSet.Builder claims = baseClaims(issuer, audience, subject, expiration, nonce);
		if (email != null) {
			claims.claim("email", email).claim("email_verified", emailVerified);
		}
		return sign(signingKey, claims.build());
	}

	private String signedTokenWithStringEmailVerified(
			RSAKey signingKey,
			String issuer,
			String audience,
			String subject,
			Instant expiration,
			String email,
			String emailVerified,
			String nonce) throws JOSEException {
		JWTClaimsSet.Builder claims = baseClaims(issuer, audience, subject, expiration, nonce)
				.claim("email", email)
				.claim("email_verified", emailVerified);
		return sign(signingKey, claims.build());
	}

	private JWTClaimsSet.Builder baseClaims(String issuer, String audience, String subject, Instant expiration, String nonce) {
		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
				.issuer(issuer)
				.audience(audience)
				.expirationTime(Date.from(expiration))
				.claim("nonce", nonce);
		if (subject != null) {
			claims.subject(subject);
		}
		return claims;
	}

	private String sign(RSAKey signingKey, JWTClaimsSet claims) throws JOSEException {
		SignedJWT signedJwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
		signedJwt.sign(new RSASSASigner(signingKey));
		return signedJwt.serialize();
	}

	private String sha256Hex(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
