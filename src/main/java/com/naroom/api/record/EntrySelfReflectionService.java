package com.naroom.api.record;

import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntrySelfReflection;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import com.naroom.api.record.dto.EntrySelfReflectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EntrySelfReflectionService {

	private final EntryRepository entryRepository;
	private final EntrySelfReflectionRepository entrySelfReflectionRepository;

	public EntrySelfReflectionService(
			EntryRepository entryRepository, EntrySelfReflectionRepository entrySelfReflectionRepository) {
		this.entryRepository = entryRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
	}

	public List<EntrySelfReflectionResponse> listReflections(UUID memberId, UUID entryId) {
		getOwnedEntryOrThrow(memberId, entryId);
		return entrySelfReflectionRepository.findByEntry_IdOrderByCreatedAtDesc(entryId).stream()
				.map(EntrySelfReflectionResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public EntrySelfReflectionResponse createReflection(UUID memberId, UUID entryId, String content) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		EntrySelfReflection reflection = EntrySelfReflection.create(entry, content);
		return EntrySelfReflectionResponse.from(entrySelfReflectionRepository.save(reflection));
	}

	@Transactional
	public EntrySelfReflectionResponse updateReflection(UUID memberId, UUID entryId, UUID reflectionId, String content) {
		getOwnedEntryOrThrow(memberId, entryId);
		EntrySelfReflection reflection = entrySelfReflectionRepository.findById(reflectionId)
				.filter(candidate -> candidate.getEntry().getId().equals(entryId))
				.orElseThrow(() -> new BusinessException(RecordErrorCode.SELF_REFLECTION_NOT_FOUND));
		reflection.update(content);
		return EntrySelfReflectionResponse.from(reflection);
	}

	private Entry getOwnedEntryOrThrow(UUID memberId, UUID entryId) {
		return entryRepository.findByIdAndMember_Id(entryId, memberId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
	}

}
