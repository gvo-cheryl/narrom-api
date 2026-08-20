package com.naroom.api.admin.domain.repository;

import com.naroom.api.admin.domain.entity.AdminSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminSessionRepository extends JpaRepository<AdminSession, UUID> {

	// AdminSessionAuthenticationFilter가 트랜잭션 밖(open-in-view: false)에서 adminUser와 그 roles를 바로
	// 읽는다 - LAZY 그대로면 LazyInitializationException이 나므로 이 조회에서만 즉시 로딩한다.
	// EntityGraph(FETCH 타입, 기본값)는 명시하지 않은 연관을 전부 LAZY로 덮어쓰므로, AdminUser.roles가
	// @ElementCollection(fetch = EAGER)이어도 "adminUser.roles"를 별도로 명시해야 한다.
	@EntityGraph(attributePaths = {"adminUser", "adminUser.roles"})
	Optional<AdminSession> findBySessionTokenHash(String sessionTokenHash);

}
