package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.TagSource;
import com.naroom.api.record.domain.entity.TagState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryTagRepository extends JpaRepository<EntryTag, UUID> {

	List<EntryTag> findByEntry_Id(UUID entryId);

	Optional<EntryTag> findByEntry_IdAndTag_Id(UUID entryId, UUID tagId);

	List<EntryTag> findByEntry_IdAndSource(UUID entryId, TagSource source);

	void deleteByEntry_IdAndSource(UUID entryId, TagSource source);

	List<EntryTag> findByEntry_IdInAndStateIn(Collection<UUID> entryIds, Collection<TagState> states);

	List<EntryTag> findByEntry_Member_IdAndStateIn(UUID memberId, Collection<TagState> states);

	List<EntryTag> findByEntry_Member_IdAndTag_IdAndStateIn(UUID memberId, UUID tagId, Collection<TagState> states);

}
