package com.naroom.api.ai.prompt;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiReflection;
import com.naroom.api.ai.domain.entity.MemberAiPreference;
import com.naroom.api.ai.domain.repository.AiReflectionRepository;
import com.naroom.api.ai.domain.repository.MemberAiPreferenceRepository;
import com.naroom.api.checkin.domain.entity.CheckIn;
import com.naroom.api.checkin.domain.repository.CheckInRepository;
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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 14장 5단계 조립 구조 중 1~2단계(공통 지침·기능별 지침)는 AiPromptResolver가 담당하고(관리자가 발행한
// 지침이 있으면 그것을, 없으면 AiInstructionCatalog 코드 기본값을 쓴다), 이 클래스는 3~4단계(회원
// 선호도·현재 요청 맥락)를 실제 데이터로 채운다. 5단계(출력 스키마 버전)는 여전히 AiInstructionCatalog다.
@Service
@Transactional(readOnly = true)
public class PromptAssembler {

	private static final Set<TagState> CONFIRMED_TAG_STATES = EnumSet.of(TagState.CONFIRMED, TagState.SYSTEM);

	private final EntryRepository entryRepository;
	private final EntryTagRepository entryTagRepository;
	private final EntrySelfReflectionRepository entrySelfReflectionRepository;
	private final MemberAiPreferenceRepository memberAiPreferenceRepository;
	private final CheckInRepository checkInRepository;
	private final AiReflectionRepository aiReflectionRepository;
	private final AiPromptResolver aiPromptResolver;

	public PromptAssembler(
			EntryRepository entryRepository,
			EntryTagRepository entryTagRepository,
			EntrySelfReflectionRepository entrySelfReflectionRepository,
			MemberAiPreferenceRepository memberAiPreferenceRepository,
			CheckInRepository checkInRepository,
			AiReflectionRepository aiReflectionRepository,
			AiPromptResolver aiPromptResolver) {
		this.entryRepository = entryRepository;
		this.entryTagRepository = entryTagRepository;
		this.entrySelfReflectionRepository = entrySelfReflectionRepository;
		this.memberAiPreferenceRepository = memberAiPreferenceRepository;
		this.checkInRepository = checkInRepository;
		this.aiReflectionRepository = aiReflectionRepository;
		this.aiPromptResolver = aiPromptResolver;
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

		AiPromptResolver.ResolvedCommonInstructions common = aiPromptResolver.resolveCommon();
		AiPromptResolver.ResolvedFeatureInstructions feature = aiPromptResolver.resolveFeature(AiFeatureType.ENTRY_REFLECTION);

		return new AssembledPrompt(
				common.versionLabel(),
				common.content(),
				feature.versionLabel(),
				feature.content(),
				buildPreferenceInstructions(preference),
				buildContextContent(entry, confirmedTags, selfReflections),
				AiInstructionCatalog.outputSchemaVersion(AiFeatureType.ENTRY_REFLECTION),
				feature.modelName(),
				feature.outputMaxLength());
	}

	// 9.2절: 확정(CONFIRMED) 또는 시스템 적용(SYSTEM) 태그만 "확정 태그"로 취급한다. SUGGESTED는 아직 사용자
	// 확인 전이고 REJECTED는 사용자가 거부한 것이라 둘 다 현재 맥락에 포함하지 않는다.
	private boolean isConfirmed(EntryTag entryTag) {
		return entryTag.getState() == TagState.CONFIRMED || entryTag.getState() == TagState.SYSTEM;
	}

	// 3단계 3-B: §12.3 우선순위(1.사용자 선택 감정·강도 2.에너지 3.확정 태그 4.자기정리 5.개별 기록 AI 요약
	// 6.도움이 된 행동 7.선별 원문 8.근거 entryId 9.회원 선호도)를 따르되, 1차 구현이라 6(도움이 된 행동 평가)과
	// 7의 정교한 선별은 적용하지 않는다 - evidenceEntries 전체(3-A가 이미 "발행된 기록 전부"로 단순화한 것)를
	// 원문으로 포함하고, 회원 선호도는 기존 buildPreferenceInstructions를 그대로 재사용한다.
	public AssembledPrompt assembleForPeriodReflection(
			Member member, AiFeatureType featureType, LocalDate periodStart, LocalDate periodEnd, List<Entry> evidenceEntries) {
		List<UUID> entryIds = evidenceEntries.stream().map(Entry::getId).toList();

		List<CheckIn> checkIns = checkInRepository.findByMember_IdAndCheckInDateBetween(member.getId(), periodStart, periodEnd);

		Map<UUID, List<EntryTag>> confirmedTagsByEntry = entryTagRepository
				.findByEntry_IdInAndStateIn(entryIds, CONFIRMED_TAG_STATES).stream()
				.collect(Collectors.groupingBy(tag -> tag.getEntry().getId()));

		Map<UUID, List<EntrySelfReflection>> selfReflectionsByEntry = entrySelfReflectionRepository
				.findByEntry_IdIn(entryIds).stream()
				.collect(Collectors.groupingBy(reflection -> reflection.getEntry().getId()));

		Map<UUID, String> aiSummaryByEntry = aiReflectionRepository.findByEntry_IdIn(entryIds).stream()
				.filter(reflection -> reflection.getReflectionText() != null)
				.collect(Collectors.toMap(
						reflection -> reflection.getEntry().getId(),
						AiReflection::getReflectionText,
						(first, second) -> second));

		MemberAiPreference preference = memberAiPreferenceRepository.findByMember_Id(member.getId()).orElse(null);

		AiPromptResolver.ResolvedCommonInstructions common = aiPromptResolver.resolveCommon();
		AiPromptResolver.ResolvedFeatureInstructions feature = aiPromptResolver.resolveFeature(featureType);

		return new AssembledPrompt(
				common.versionLabel(),
				common.content(),
				feature.versionLabel(),
				feature.content(),
				buildPreferenceInstructions(preference),
				buildPeriodContextContent(
						periodStart, periodEnd, checkIns, evidenceEntries,
						confirmedTagsByEntry, selfReflectionsByEntry, aiSummaryByEntry),
				AiInstructionCatalog.outputSchemaVersion(featureType),
				feature.modelName(),
				feature.outputMaxLength());
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

	private String buildPeriodContextContent(
			LocalDate periodStart,
			LocalDate periodEnd,
			List<CheckIn> checkIns,
			List<Entry> evidenceEntries,
			Map<UUID, List<EntryTag>> confirmedTagsByEntry,
			Map<UUID, List<EntrySelfReflection>> selfReflectionsByEntry,
			Map<UUID, String> aiSummaryByEntry) {
		StringBuilder sb = new StringBuilder();
		sb.append("[기간] ").append(periodStart).append(" ~ ").append(periodEnd).append('\n');

		// §12.3 우선순위 1~2: 사용자가 직접 선택한 감정 강도·에너지 수치.
		if (!checkIns.isEmpty()) {
			sb.append("[체크인 감정·에너지 추이]\n");
			checkIns.stream()
					.sorted(Comparator.comparing(CheckIn::getCheckInDate))
					.forEach(checkIn -> sb.append("- ")
							.append(checkIn.getCheckInDate())
							.append(": 감정 강도=").append(checkIn.getEmotionIntensity())
							.append(", 에너지=").append(checkIn.getEnergyLevel())
							.append('\n'));
		}

		for (Entry entry : evidenceEntries) {
			sb.append("[기록 ").append(entry.getRecordDate()).append(" / ").append(entry.getEntryType())
					.append(" / id=").append(entry.getId()).append("]\n");
			if (entry.getBody() != null && !entry.getBody().isBlank()) {
				sb.append(entry.getBody()).append('\n');
			}
			List<EntryTag> confirmedTags = confirmedTagsByEntry.getOrDefault(entry.getId(), List.of());
			if (!confirmedTags.isEmpty()) {
				sb.append("  확정 태그: ")
						.append(confirmedTags.stream().map(tag -> tag.getTag().getName()).collect(Collectors.joining(", ")))
						.append('\n');
			}
			String aiSummary = aiSummaryByEntry.get(entry.getId());
			if (aiSummary != null) {
				sb.append("  개별 기록 AI 요약: ").append(aiSummary).append('\n');
			}
			List<EntrySelfReflection> selfReflections = selfReflectionsByEntry.getOrDefault(entry.getId(), List.of());
			if (!selfReflections.isEmpty()) {
				sb.append("  사용자가 직접 작성한 자기정리: ")
						.append(selfReflections.stream().map(EntrySelfReflection::getContent).collect(Collectors.joining(" / ")))
						.append('\n');
			}
		}
		return sb.toString();
	}

}
