package com.naroom.api.ai.result;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// 9.1절 백엔드 재검증 원칙을 기간별 회고에도 그대로 적용한다: evidenceEntryIds는 실제로 이 회고에 넘긴
// 근거 기록 집합(allowedEvidenceEntryIds)에 있는 것만 남긴다 - 회원 소유 여부만 보는 개별 기록 회고보다
// 한 단계 더 엄격하다(다른 기간의 내 기록 ID를 모델이 잘못 인용해도 걸러진다).
@Service
public class PeriodReflectionResponseParser {

	private final ObjectMapper objectMapper = new ObjectMapper();

	public PeriodReflectionResult parse(String outputJson, Set<UUID> allowedEvidenceEntryIds) {
		RawPeriodReflection raw = readRaw(outputJson);
		if (raw.summary() == null || raw.summary().isBlank()) {
			throw new IllegalArgumentException("AI 응답의 summary가 비어 있습니다");
		}
		if (raw.reflectionQuestion() == null || raw.reflectionQuestion().isBlank()) {
			throw new IllegalArgumentException("AI 응답의 reflectionQuestion이 비어 있습니다");
		}

		List<UUID> evidenceEntryIds = orEmpty(raw.evidenceEntryIds()).stream()
				.map(this::parseUuidOrNull)
				.filter(Objects::nonNull)
				.filter(allowedEvidenceEntryIds::contains)
				.toList();

		return new PeriodReflectionResult(
				raw.summary(),
				orEmpty(raw.repeatedEmotionsAndSituations()),
				orEmpty(raw.difficultMoments()),
				orEmpty(raw.gratefulMoments()),
				orEmpty(raw.triedResponses()),
				orEmpty(raw.helpfulConditions()),
				raw.reflectionQuestion(),
				evidenceEntryIds,
				parseSafetyGradeOrNull(raw.safetyStatus()));
	}

	private RawPeriodReflection readRaw(String outputJson) {
		try {
			return objectMapper.readValue(outputJson, RawPeriodReflection.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("AI 응답이 지정한 스키마의 JSON이 아닙니다", e);
		}
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

	private record RawPeriodReflection(
			String summary,
			List<String> repeatedEmotionsAndSituations,
			List<String> difficultMoments,
			List<String> gratefulMoments,
			List<String> triedResponses,
			List<String> helpfulConditions,
			String reflectionQuestion,
			List<String> evidenceEntryIds,
			String safetyStatus) {
	}

}
