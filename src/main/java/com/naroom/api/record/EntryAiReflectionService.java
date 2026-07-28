package com.naroom.api.record;

import com.naroom.api.ai.domain.entity.AiJob;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.repository.AiJobRepository;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.dto.EntryAiReflectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 5-A(§24.5 "AI 처리 중·실패·안전 화면"): AiJob이 진행 상태의 근거고, AiReflection은 완료된 뒤에만 생기는
// 결과 내용이다. 두 개를 조합해야 "아직 처리 중"부터 "완료된 결과 보기"까지 한 번에 보여줄 수 있다.
@Service
@Transactional(readOnly = true)
public class EntryAiReflectionService {

	private final EntryRepository entryRepository;
	private final AiJobRepository aiJobRepository;
	private final AiReflectionRepository aiReflectionRepository;

	public EntryAiReflectionService(
			EntryRepository entryRepository, AiJobRepository aiJobRepository, AiReflectionRepository aiReflectionRepository) {
		this.entryRepository = entryRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiReflectionRepository = aiReflectionRepository;
	}

	public EntryAiReflectionResponse getStatus(UUID memberId, UUID entryId) {
		if (entryRepository.findByIdAndMember_Id(entryId, memberId).isEmpty()) {
			throw new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND);
		}
		AiJob job = aiJobRepository.findFirstByEntry_IdOrderByCreatedAtDesc(entryId).orElse(null);
		if (job == null) {
			return EntryAiReflectionResponse.notRequested();
		}
		AiReflection reflection = aiReflectionRepository.findByEntry_IdOrderByVersionNoDesc(entryId).stream()
				.findFirst()
				.orElse(null);
		return EntryAiReflectionResponse.from(job, reflection);
	}

}
