package com.naroom.api.lifetime.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.lifetime.domain.entity.PeriodReflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodReflectionRepository extends JpaRepository<PeriodReflection, UUID> {

	List<PeriodReflection> findByMember_IdAndFeatureTypeAndPeriodStartOrderByVersionNoDesc(
			UUID memberId, AiFeatureType featureType, LocalDate periodStart);

	Optional<PeriodReflection> findByEntry_Id(UUID entryId);

	Optional<PeriodReflection> findByIdAndMember_Id(UUID id, UUID memberId);

	// 지난 회고 목록(히스토리) 조회용. 재생성으로 같은 기간에 버전이 여러 개 있을 수 있어, versionNo desc를
	// 2차 정렬 기준으로 둬서 같은 periodStart 안에서는 항상 최신 버전이 먼저 나오게 한다 - 서비스 계층에서
	// periodStart+featureType당 처음 나온(=최신 버전) 것만 남긴다.
	List<PeriodReflection> findByMember_IdOrderByPeriodStartDescVersionNoDesc(UUID memberId);

	List<PeriodReflection> findByMember_IdAndFeatureTypeOrderByPeriodStartDescVersionNoDesc(
			UUID memberId, AiFeatureType featureType);

	// AI 잡 처리(PeriodReflectionJobProcessor)는 트랜잭션이 끝난 뒤에도 member를 읽어야 하므로
	// join fetch로 지연 로딩 프록시가 아닌 실제 값을 가져온다(LazyInitializationException 방지).
	@Query("select pr from PeriodReflection pr join fetch pr.member where pr.entry.id = :entryId")
	Optional<PeriodReflection> findByEntry_IdWithMember(UUID entryId);

}
