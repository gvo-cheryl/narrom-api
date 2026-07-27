package com.naroom.api.ai.prompt;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.MemberAiPreference;
import com.naroom.api.ai.domain.repository.MemberAiPreferenceRepository;
import com.naroom.api.global.error.exception.BusinessException;
import com.naroom.api.record.domain.entity.Entry;
import com.naroom.api.record.domain.entity.EntrySelfReflection;
import com.naroom.api.record.domain.entity.EntryTag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagState;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.EntrySelfReflectionRepository;
import com.naroom.api.record.domain.repository.EntryTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// 14장 5단계 조립 구조 중 1~2·5단계(공통 지침·기능별 지침·출력 스키마 버전)는 AiInstructionCatalog가 담당하고,
// 이 클래스는 3~4단계(회원 선호도·현재 요청 맥락)를 실제 데이터로 채운다.
@Service
@Transactional(readOnly = true)
public class PromptAssembler {

	private final EntryRepository entryRepository;
	private final EntryTagRepository entryTagRepository;
	private final EntrySelfReflectionRepository entrySelfReflectionRepository;
	private final MemberAiPreferenceRepository memberAiPreferenceRepository;

	public PromptAssembler(
			EntryRepository entryRepository,
			EntryTagRepository entryTagRepository,
			EntrySelfReflectionRepository entrySelfReflectionRepository,
			MemberAiPreferenceRepository memberAiPreferenceRepository) {
		this.entryRepository = entryRepository;
		this.entryTagRepository = entryTagRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
		this.memberAiPreferenceRepository = memberAiPreferenceRepository;
	}

	public AssembledPrompt assembleForEntryReflection(UUID entryId) {
		Entry entry = entryRepository.findById(entryId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.ENTRY_NOT_FOUND));
		// 3.3절: 사용자가 기록별로 AI 분석을 끌 수 있다. 이 시점까지 오면 안 되는 상태라 방어적으로 막는다.
		if (!entry.isAiProcessingAllowed()) {
			throw new IllegalStateException("Entry " + entryId + " has AI processing disabled");
		}

		List<EntryTag> confirmedTags = entryTagRepository.findByEntry_Id(entryId).stream()
				.filter(this::isConfirmed)
				.toList();
		List<EntrySelfReflection> selfReflections =
				entrySelfReflectionRepository.findByEntry_IdOrderByCreatedAtDesc(entryId);
		MemberAiPreference preference =
				memberAiPreferenceRepository.findByMember_Id(entry.getMember().getId()).orElse(null);

		return new AssembledPrompt(
				AiInstructionCatalog.COMMON_INSTRUCTIONS_VERSION,
				AiInstructionCatalog.COMMON_INSTRUCTIONS,
				AiInstructionCatalog.featureInstructionsVersion(AiFeatureType.ENTRY_REFLECTION),
				AiInstructionCatalog.featureInstructions(AiFeatureType.ENTRY_REFLECTION),
				buildPreferenceInstructions(preference),
				buildContextContent(entry, confirmedTags, selfReflections),
				AiInstructionCatalog.outputSchemaVersion(AiFeatureType.ENTRY_REFLECTION));
	}

	// 9.2절: 확정(CONFIRMED) 또는 시스템 적용(SYSTEM) 태그만 "확정 태그"로 취급한다. SUGGESTED는 아직 사용자
	// 확인 전이고 REJECTED는 사용자가 거부한 것이라 둘 다 현재 맥락에 포함하지 않는다.
	private boolean isConfirmed(EntryTag entryTag) {
		return entryTag.getState() == TagState.CONFIRMED || entryTag.getState() == TagState.SYSTEM;
	}

	// 14.3절: 회원 선호도는 공통 안전 규칙(금지 원칙)보다 우선할 수 없다. 이 레이어는 스타일 지시만 담고,
	// 금지 원칙은 항상 별도 레이어(COMMON_INSTRUCTIONS)로 함께 전달되므로 이 레이어가 그것을 덮어쓰지 않는다.
	private String buildPreferenceInstructions(MemberAiPreference preference) {
		if (preference == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if (preference.getTone() != null) {
			sb.append("말투 선호: ").append(preference.getTone()).append('\n');
		}
		if (preference.getResponseLength() != null) {
			sb.append("응답 길이 선호: ").append(preference.getResponseLength()).append('\n');
		}
		if (preference.isReduceEmotionalValidation()) {
			sb.append("위로·공감 표현을 줄이고 핵심 위주로 전달할 것\n");
		}
		return sb.toString();
	}

	private String buildContextContent(Entry entry, List<EntryTag> confirmedTags, List<EntrySelfReflection> selfReflections) {
		StringBuilder sb = new StringBuilder();
		sb.append("[기록 유형] ").append(entry.getEntryType()).append('\n');
		if (entry.getTitle() != null) {
			sb.append("[제목] ").append(entry.getTitle()).append('\n');
		}
		sb.append("[본문]\n").append(entry.getBody()).append('\n');

		List<EntryTag> emotionTags = confirmedTags.stream()
				.filter(tag -> tag.getTag().getCategory() == TagCategory.EMOTION)
				.toList();
		List<EntryTag> otherTags = confirmedTags.stream()
				.filter(tag -> tag.getTag().getCategory() != TagCategory.EMOTION)
				.toList();
		if (!emotionTags.isEmpty()) {
			sb.append("[사용자가 확정한 감정] ")
					.append(emotionTags.stream().map(tag -> tag.getTag().getName()).collect(Collectors.joining(", ")))
					.append('\n');
		}
		if (!otherTags.isEmpty()) {
			sb.append("[사용자가 확정한 태그] ")
					.append(otherTags.stream().map(tag -> tag.getTag().getName()).collect(Collectors.joining(", ")))
					.append('\n');
		}
		if (!selfReflections.isEmpty()) {
			sb.append("[사용자가 직접 작성한 자기정리]\n");
			selfReflections.forEach(reflection -> sb.append("- ").append(reflection.getContent()).append('\n'));
		}
		return sb.toString();
	}

}
