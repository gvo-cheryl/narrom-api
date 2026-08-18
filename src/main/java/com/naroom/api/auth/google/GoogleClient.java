package com.naroom.api.auth.google;

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
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Google ID Token(RS256, JWKS 서명) 검증. Google access token은 회원 식별 근거로 쓰지 않는다
// (authentication.md 원칙과 동일하게 provider가 이미 검증한 자격 증명만 신뢰한다).
@Component
public class GoogleClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleClient.class);
	private static final Set<String> VALID_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
	private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

	private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
	private final Set<String> allowedAudiences;

	@Autowired
	public GoogleClient(GoogleAuthProperties properties) {
		this(properties, defaultJwtProcessor());
	}

	// 테스트에서 실제 네트워크 JWKS 대신 로컬 키로 서명 검증하도록 processor를 주입할 수 있는 통로.
	GoogleClient(GoogleAuthProperties properties, ConfigurableJWTProcessor<SecurityContext> jwtProcessor) {
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
			throw new IllegalStateException("Invalid Google JWKS URI: " + JWKS_URI, ex);
		}
	}

	public GoogleUserInfo verify(String idToken) {
		JWTClaimsSet claims;
		try {
			claims = jwtProcessor.process(idToken, null);
		} catch (BadJOSEException ex) {
			log.warn("google id token rejected: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		} catch (ParseException ex) {
			log.warn("google id token malformed: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		} catch (JOSEException ex) {
			log.warn("google id token verification unavailable: {}", ex.getMessage());
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_UNAVAILABLE);
		}

		if (!VALID_ISSUERS.contains(claims.getIssuer())) {
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
			String sub = claims.getSubject();
			if (sub == null || sub.isBlank()) {
				throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
			}
			return new GoogleUserInfo(
					sub,
					claims.getStringClaim("email"),
					Boolean.TRUE.equals(claims.getBooleanClaim("email_verified")),
					claims.getStringClaim("name"),
					claims.getStringClaim("picture"));
		} catch (ParseException ex) {
			throw new BusinessException(AuthErrorCode.AUTH_PROVIDER_TOKEN_INVALID);
		}
	}

}
