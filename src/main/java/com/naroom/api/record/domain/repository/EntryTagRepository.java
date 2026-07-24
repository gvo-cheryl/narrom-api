package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EntryTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryTagRepository extends JpaRepository<EntryTag, UUID> {

	List<EntryTag> findByEntry_Id(UUID entryId);

	Optional<EntryTag> findByEntry_IdAndTag_Id(UUID entryId, UUID tagId);

}
