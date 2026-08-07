package com.naroom.api.account.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 값은 application.yml(naroom.push.*)에서 오고, key는 ${PUSH_TOKEN_ENCRYPTION_KEY} placeholder로 위임된다.
// TODO: docs/instruction/notification/Naroom_Beta1_알림_기획_설계.md DEC-03 참고 - Supabase Vault
// (pgsodium) 암호화 모듈이 AI 도메인 4단계에서 만들어지면 이 임시 대칭키 방식을 그걸로 교체한다.
@ConfigurationProperties(prefix = "naroom.push")
public record PushTokenEncryptionProperties(String tokenEncryptionKey) {
}
