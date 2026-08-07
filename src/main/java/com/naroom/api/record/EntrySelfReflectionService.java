package com.naroom.api.record;

import com.naroom.api.ai.domain.entity.AiJobStatus;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.error.AiErrorCode;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.badge.BadgeAwardService;
import com.naroom.api.badge.domain.entity.BadgeCode;
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

	// §7(뱃지 설계) 자기정리형 SELF_REFLECTION_5의 누적 기준값.
	private static final long SELF_REFLECTION_MILESTONE = 5;

	private final EntryRepository entryRepository;
	private final EntrySelfReflectionRepository entrySelfReflectionRepository;
	private final AiReflectionRepository aiReflectionRepository;
	private final BadgeAwardService badgeAwardService;

	public EntrySelfReflectionService(
			EntryRepository entryRepository,
			EntrySelfReflectionRepository entrySelfReflectionRepository,
			AiReflectionRepository aiReflectionRepository,
			BadgeAwardService badgeAwardService) {
		this.entryRepository = entryRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
		this.aiReflectionRepository = aiReflectionRepository;
		this.badgeAwardService = badgeAwardService;
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
		EntrySelfReflection saved = entrySelfReflectionRepository.save(reflection);
		awardSelfReflectionBadges(memberId);
		return EntrySelfReflectionResponse.from(saved);
	}

	// §7(뱃지 설계) 자기정리형 FIRST_SELF_REFLECTION + SELF_REFLECTION_5. count는 방금 저장한 것을
	// 포함한 값이다.
	private void awardSelfReflectionBadges(UUID memberId) {
		badgeAwardService.award(memberId, BadgeCode.FIRST_SELF_REFLECTION);
		long count = entrySelfReflectionRepository.countByEntry_Member_Id(memberId);
		if (count == SELF_REFLECTION_MILESTONE) {
			badgeAwardService.award(memberId, BadgeCode.SELF_REFLECTION_5);
		}
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
