package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

	Optional<Entry> findByIdAndMember_Id(UUID id, UUID memberId);

	List<Entry> findByMember_IdOrderByRecordDateDescCreatedAtDesc(UUID memberId);

	List<Entry> findByMember_IdAndEntryTypeOrderByRecordDateDescCreatedAtDesc(UUID memberId, EntryType entryType);

	List<Entry> findByMember_IdAndRecordDateOrderByCreatedAtDesc(UUID memberId, LocalDate recordDate);

}
