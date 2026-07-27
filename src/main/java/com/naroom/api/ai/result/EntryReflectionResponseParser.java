package com.naroom.api.ai.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import com.naroom.api.record.domain.repository.EntryRepository;
import com.naroom.api.record.domain.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// 9.1절 백엔드 재검증 원칙: AI가 반환한 값(감정·태그 표현, 근거 기록 ID, 신뢰도, 안전 상태)을 그대로 신뢰하지 않고
// 여기서 서버 허용 목록·소유권·범위와 다시 대조한다. GenerationResult.outputJson()이 이 파서의 입력이다.
@Service
@Transactional(readOnly = true)
public class EntryReflectionResponseParser {

	// 이 프로젝트는 전역 ObjectMapper 빈을 등록하지 않는다(Spring MVC가 내부적으로만 사용). 여기서만 쓰는
	// 파싱 전용 인스턴스라 별도 빈으로 노출하지 않고 직접 생성한다.
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final TagRepository tagRepository;
	private final EntryRepository entryRepository;

	public EntryReflectionResponseParser(TagRepository tagRepository, EntryRepository entryRepository) {
		this.tagRepository = tagRepository;
		this.entryRepository = entryRepository;
	}

	public EntryReflectionResult parse(String outputJson, UUID memberId) {
		RawEntryReflection raw = readRaw(outputJson);
		if (raw.summary() == null || raw.summary().isBlank()) {
			throw new IllegalArgumentException("AI 응답의 summary가 비어 있습니다");
		}
		if (raw.reflectionQuestion() == null || raw.reflectionQuestion().isBlank()) {
			throw new IllegalArgumentException("AI 응답의 reflectionQuestion이 비어 있습니다");
		}

		Map<String, Tag> systemTagsByNormalizedName = tagRepository.findByScopeAndActiveTrue(TagScope.SYSTEM).stream()
				.collect(Collectors.toMap(
						tag -> normalize(tag.getNormalizedName()), tag -> tag, (first, second) -> first));

		List<EmotionCandidateResult> emotionCandidates = orEmpty(raw.emotionCandidates()).stream()
				.filter(this::hasValidConfidence)
				.map(candidate -> toEmotionCandidateResult(candidate, systemTagsByNormalizedName))
				.toList();

		List<Tag> suggestedTags = new ArrayList<>();
		List<String> unmappedTagNames = new ArrayList<>();
		for (String name : orEmpty(raw.suggestedTagNames())) {
			Tag matched = systemTagsByNormalizedName.get(normalize(name));
			if (matched != null) {
				suggestedTags.add(matched);
			} else {
				unmappedTagNames.add(name);
			}
		}

		List<UUID> evidenceEntryIds = orEmpty(raw.evidenceEntryIds()).stream()
				.map(this::parseUuidOrNull)
				.filter(Objects::nonNull)
				.filter(id -> entryRepository.findByIdAndMember_Id(id, memberId).isPresent())
				.toList();

		return new EntryReflectionResult(
				raw.summary(),
				emotionCandidates,
				suggestedTags,
				unmappedTagNames,
				raw.reflectionQuestion(),
				evidenceEntryIds,
				parseSafetyGradeOrNull(raw.safetyStatus()));
	}

	private RawEntryReflection readRaw(String outputJson) {
		try {
			return objectMapper.readValue(outputJson, RawEntryReflection.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("AI 응답이 지정한 스키마의 JSON이 아닙니다", e);
		}
	}

	// 9.1절: 신뢰도는 0~1 범위로 검증한다. 범위를 벗어나면 후보 전체를 신뢰할 수 없다고 보고 제외한다.
	private boolean hasValidConfidence(RawEmotionCandidate candidate) {
		return candidate.confidence() != null
				&& candidate.confidence().compareTo(BigDecimal.ZERO) >= 0
				&& candidate.confidence().compareTo(BigDecimal.ONE) <= 0;
	}

	private EmotionCandidateResult toEmotionCandidateResult(RawEmotionCandidate candidate, Map<String, Tag> systemTagsByNormalizedName) {
		Tag matched = systemTagsByNormalizedName.get(normalize(candidate.name()));
		Tag emotionMatch = (matched != null && matched.getCategory() == TagCategory.EMOTION) ? matched : null;
		return new EmotionCandidateResult(candidate.name(), candidate.confidence(), emotionMatch);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private UUID parseUuidOrNull(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private AiSafetyGrade parseSafetyGradeOrNull(String value) {
		if (value == null) {
			return null;
		}
		try {
			return AiSafetyGrade.valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private <T> List<T> orEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}

	private record RawEntryReflection(
			String summary,
			List<RawEmotionCandidate> emotionCandidates,
			List<String> suggestedTagNames,
			String reflectionQuestion,
			List<String> evidenceEntryIds,
			String safetyStatus) {
	}

	private record RawEmotionCandidate(String name, BigDecimal confidence) {
	}

}
