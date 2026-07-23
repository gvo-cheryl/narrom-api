package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.SocialIdentity;
import com.naroom.api.account.domain.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, UUID> {

	// 카카오 로그인 시 기존 연결 여부 확인 (authentication.md 카카오 로그인 처리 순서).
	Optional<SocialIdentity> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

}
