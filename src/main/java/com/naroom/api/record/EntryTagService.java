package com.naroom.api.record;

import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import com.naroom.api.record.dto.EntryTagResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EntryTagService {

	private final EntryRepository entryRepository;
	private final EntryTagRepository entryTagRepository;
	private final TagService tagService;

	public EntryTagService(EntryRepository entryRepository, EntryTagRepository entryTagRepository, TagService tagService) {
		this.entryRepository = entryRepository;
		this.entryTagRepository = entryTagRepository;
		this.tagService = tagService;
	}

	public List<EntryTagResponse> listEntryTags(UUID memberId, UUID entryId) {
		getOwnedEntryOrThrow(memberId, entryId);
		return entryTagRepository.findByEntry_Id(entryId).stream()
				.map(EntryTagResponse::from)
				.collect(Collectors.toList());
	}

	// 이미 같은 태그가 붙어 있으면 중복 생성하지 않고 기존 연결을 반환한다.
	@Transactional
	public EntryTagResponse attachUserTag(UUID memberId, UUID entryId, UUID tagId) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		Tag tag = tagService.getTagOrThrow(tagId);
		return entryTagRepository.findByEntry_IdAndTag_Id(entryId, tagId)
				.map(EntryTagResponse::from)
				.orElseGet(() -> EntryTagResponse.from(entryTagRepository.save(EntryTag.attachByUser(entry, tag))));
	}

	@Transactional
	public EntryTagResponse confirmTag(UUID memberId, UUID entryId, UUID entryTagId) {
		EntryTag entryTag = getOwnedEntryTagOrThrow(memberId, entryId, entryTagId);
		entryTag.confirm();
		return EntryTagResponse.from(entryTag);
	}

	@Transactional
	public EntryTagResponse rejectTag(UUID memberId, UUID entryId, UUID entryTagId) {
		EntryTag entryTag = getOwnedEntryTagOrThrow(memberId, entryId, entryTagId);
		entryTag.reject();
		return EntryTagResponse.from(entryTag);
	}

	private EntryTag getOwnedEntryTagOrThrow(UUID memberId, UUID entryId, UUID entryTagId) {
		getOwnedEntryOrThrow(memberId, entryId);
		EntryTag entryTag = entryTagRepository.findById(entryTagId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_TAG_NOT_FOUND));
		if (!entryTag.getEntry().getId().equals(entryId)) {
			throw new BusinessException(RecordErrorCode.ENTRY_TAG_NOT_FOUND);
		}
		return entryTag;
	}

	private Entry getOwnedEntryOrThrow(UUID memberId, UUID entryId) {
		return entryRepository.findByIdAndMember_Id(entryId, memberId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
	}

}
