package com.naroom.api.auth.apple;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Apple identity token(RS256, JWKS 서명) 검증. rawNonce는 필수로 받아 SHA-256 해시가 identity token의
// nonce claim과 일치하는지 확인한다(Apple 공식 가이드: 클라이언트는 원문 nonce의 SHA-256 해시를 authorization
// 요청에 실어 보내고, Apple은 그 해시값을 그대로 identity token의 nonce claim에 넣는다).
@Component
public class AppleClient {

	private static final Logger log = LoggerFactory.getLogger(AppleClient.class);
	private static final String ISSUER = "https://appleid.apple.com";
	private static final String JWKS_URI = "https://appleid.apple.com/auth/keys";

	private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
	private final Set<String> allowedAudiences;

	@Autowired
	public AppleClient(AppleAuthProperties properties) {
		this(properties, defaultJwtProcessor());
	}

	// 테스트에서 실제 네트워크 JWKS 대신 로컬 키로 서명 검증하도록 processor를 주입할 수 있는 통로.
	AppleClient(AppleAuthProperties properties, ConfigurableJWTProcessor<SecurityContext> jwtProcessor) {
		this.allowedAudiences = properties.allowedAudiences() == null
				? Set.of()
				: properties.allowedAudiences().stream()
						.filter(audience -> audience != null && !audience.isBlank())
						.collect(Collectors.toUnmodifiableSet());
		this.jwtProcessor = jwtProcessor;
	}

	private static ConfigurableJWTProcessor<SecurityContext> defaultJwtProcessor() {
		try {
			ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
			JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(URI.create(JWKS_URI).toURL());
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
			processor.setJWSKeySelector(keySelector);
			return processor;
		} catch (MalformedURLException ex) {
			throw new IllegalStateException("Invalid Apple JWKS URI: " + JWKS_URI, ex);
		}
	}

	public AppleUserInfo verify(String identityToken, String rawNonce) {
		JWTClaimsSet claims;
		try {
			claims = jwtProcessor.process(identityToken, null);
		} catch (BadJOSEException ex) {
			log.warn("apple identity token rejected: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		} catch (ParseException ex) {
			log.warn("apple identity token malformed: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		} catch (JOSEException ex) {
			log.warn("apple identity token verification unavailable: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_UNAVAILABLE);
		}

		if (!ISSUER.equals(claims.getIssuer())) {
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		}
		List<String> audience = claims.getAudience();
		if (audience == null || audience.stream().noneMatch(allowedAudiences::contains)) {
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		}
		Date expiration = claims.getExpirationTime();
		if (expiration == null || expiration.before(new Date())) {
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		}

		try {
			String expectedNonce = sha256Hex(rawNonce);
			if (!expectedNonce.equals(claims.getStringClaim("nonce"))) {
				throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
			}

			String sub = claims.getSubject();
			if (sub == null || sub.isBlank()) {
				throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
			}
			return new AppleUserInfo(sub, claims.getStringClaim("email"), parseBoolean(claims.getClaim("email_verified")));
		} catch (ParseException ex) {
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		}
	}

	// Apple identity token은 email_verified/is_private_email을 JSON boolean 또는 문자열("true"/"false")
	// 둘 다로 내려보낼 수 있다(발급 시점의 Apple 라이브러리 버전에 따라 다름).
	private static boolean parseBoolean(Object claim) {
		if (claim instanceof Boolean bool) {
			return bool;
		}
		if (claim instanceof String str) {
			return Boolean.parseBoolean(str);
		}
		return false;
	}

	private static String sha256Hex(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 must be available on every JVM", ex);
		}
	}

}
