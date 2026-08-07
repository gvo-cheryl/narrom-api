package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EntrySelfReflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EntrySelfReflectionRepository extends JpaRepository<EntrySelfReflection, UUID> {

	List<EntrySelfReflection> findByEntry_IdOrderByCreatedAtDesc(UUID entryId);

	List<EntrySelfReflection> findByEntry_IdIn(Collection<UUID> entryIds);

	// 뱃지 판정(자기정리형 SELF_REFLECTION_5)이 회원 전체 누적 개수를 셀 때 쓴다.
	long countByEntry_Member_Id(UUID memberId);

}
