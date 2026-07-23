package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.MemberConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// 온보딩 완료 시 매번 새로 저장하기만 하는 이력 테이블이라 커스텀 조회가 아직 없다.
public interface MemberConsentRepository extends JpaRepository<MemberConsent, UUID> {
}
