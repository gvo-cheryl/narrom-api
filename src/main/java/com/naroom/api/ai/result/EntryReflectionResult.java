package com.naroom.api.ai.result;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;
import com.naroom.api.record.domain.entity.Tag;

import java.util.List;
import java.util.UUID;

// 백엔드 재검증을 거친 뒤의 결과다: emotionCandidates는 매칭 여부와 무관하게 전부 담고(표시용),
// suggestedTags는 서버 허용 목록(SYSTEM 태그)과 매칭에 성공한 것만, unmappedTagNames는 매칭 실패분만 담는다.
// evidenceEntryIds는 현재 회원 소유로 재확인된 것만 남는다(9.1절: AI가 반환한 DB ID를 신뢰하지 않고 재검증).
public record EntryReflectionResult(
		String summary,
		List<EmotionCandidateResult> emotionCandidates,
		List<Tag> suggestedTags,
		List<String> unmappedTagNames,
		String reflectionQuestion,
		List<UUID> evidenceEntryIds,
		AiSafetyGrade modelReportedSafetyStatus) {
}
