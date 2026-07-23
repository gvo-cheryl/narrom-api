package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// id 기반 조회(findById)만으로 auth 흐름(JWT sub)이 전부 처리되어 커스텀 쿼리가 아직 없다.
public interface MemberRepository extends JpaRepository<Member, UUID> {
}
