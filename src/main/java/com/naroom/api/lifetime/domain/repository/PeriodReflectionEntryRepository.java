package com.naroom.api.lifetime.domain.repository;

import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntry;
import com.naroom.api.lifetime.domain.entity.PeriodReflectionEntryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PeriodReflectionEntryRepository extends JpaRepository<PeriodReflectionEntry, PeriodReflectionEntryId> {

	List<PeriodReflectionEntry> findByPeriodReflection_Id(UUID periodReflectionId);

	// AI 잡 처리(PeriodReflectionJobProcessor)는 트랜잭션이 끝난 뒤에도 entry를 읽어야 하므로
	// join fetch로 지연 로딩 프록시가 아닌 실제 값을 가져온다(LazyInitializationException 방지).
	@Query("select pre from PeriodReflectionEntry pre join fetch pre.entry where pre.periodReflection.id = :periodReflectionId")
	List<PeriodReflectionEntry> findByPeriodReflection_IdWithEntry(UUID periodReflectionId);

}
