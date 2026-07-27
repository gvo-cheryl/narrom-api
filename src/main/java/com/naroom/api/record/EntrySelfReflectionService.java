package com.naroom.api.record;

import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
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
	private final AiReflectionRepository aiReflectionRepository;

	public EntrySelfReflectionService(
			EntryRepository entryRepository,
			EntrySelfReflectionRepository entrySelfReflectionRepository,
			AiReflectionRepository aiReflectionRepository) {
		this.entryRepository = entryRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
		this.aiReflectionRepository = aiReflectionRepository;
	}

	public List<EntrySelfReflectionResponse> listReflections(UUID memberId, UUID entryId) {
		getOwnedEntryOrThrow(memberId, entryId);
		return entrySelfReflectionRepository.findByEntry_IdOrderByCreatedAtDesc(entryId).stream()
				.map(EntrySelfReflectionResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public EntrySelfReflectionResponse createReflection(UUID memberId, UUID entryId, String content) {
		return createReflection(memberId, entryId, content, null);
	}

	// 4-J: aiReflectionId가 있으면 특정 AI 정리(reflectionQuestion)에 대한 사용자 의견으로 연결한다(2단계에
	// 만들어둔 EntrySelfReflection.createFromAiReflection을 처음 실제로 호출하는 지점).
	@Transactional
	public EntrySelfReflectionResponse createReflection(UUID memberId, UUID entryId, String content, UUID aiReflectionId) {
		Entry entry = getOwnedEntryOrThrow(memberId, entryId);
		EntrySelfReflection reflection = aiReflectionId == null
				? EntrySelfReflection.create(entry, content)
				: EntrySelfReflection.createFromAiReflection(entry, getOwnedAiReflectionOrThrow(entryId, aiReflectionId), content);
		return EntrySelfReflectionResponse.from(entrySelfReflectionRepository.save(reflection));
	}

	// 존재 여부를 드러내지 않기 위해 다른 기록의 것이거나 아직 COMPLETED가 아닌 경우 모두 같은 REFLECTION_NOT_FOUND로 응답한다.
	private AiReflection getOwnedAiReflectionOrThrow(UUID entryId, UUID aiReflectionId) {
		AiReflection aiReflection = aiReflectionRepository.findById(aiReflectionId)
				.orElseThrow(() -> new BusinessException(AiErrorCode.REFLECTION_NOT_FOUND));
		if (!aiReflection.getEntry().getId().equals(entryId) || aiReflection.getStatus() != AiJobStatus.COMPLETED) {
			throw new BusinessException(AiErrorCode.REFLECTION_NOT_FOUND);
		}
		return aiReflection;
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
