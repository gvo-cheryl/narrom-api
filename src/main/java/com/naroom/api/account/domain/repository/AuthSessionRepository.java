package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// 세션은 항상 JWT의 sid(=id)로 조회한다(authentication.md). refresh_token_hash로 역으로 찾는 흐름은 없다.
public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
}
