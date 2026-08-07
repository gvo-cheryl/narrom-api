package com.naroom.api.account.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

// TODO: docs/instruction/notification/Naroom_Beta1_알림_기획_설계.md DEC-03 - Supabase Vault
// (pgsodium) 모듈이 준비되면 이 임시 AES/GCM 로컬 대칭키 방식을 교체한다
// (device_installations.push_token_ciphertext 컬럼 의도와 동일).
@Component
public class PushTokenCipher {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH_BYTES = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;

	private final SecretKeySpec key;
	private final SecureRandom secureRandom = new SecureRandom();

	public PushTokenCipher(PushTokenEncryptionProperties properties) {
		byte[] keyBytes = Base64.getDecoder().decode(properties.tokenEncryptionKey());
		this.key = new SecretKeySpec(keyBytes, "AES");
	}

	public String encrypt(String plaintext) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("푸시 토큰을 암호화할 수 없습니다", e);
		}
	}

	public String decrypt(String encoded) {
		try {
			byte[] combined = Base64.getDecoder().decode(encoded);
			byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
			System.arraycopy(combined, 0, iv, 0, iv.length);
			byte[] ciphertext = new byte[combined.length - iv.length];
			System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("푸시 토큰을 복호화할 수 없습니다", e);
		}
	}

}
