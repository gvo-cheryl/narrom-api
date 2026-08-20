package com.naroom.api.ai.result;

// 9.1절/14장 "따라 읽기 금지" 규칙의 최종 방어선. 프롬프트 지침·Structured Output description만으로는
// 모델이 규칙을 어길 수 있어, 생성된 summary가 원문 맥락(contextContent)의 한 구간을 거의 그대로 옮겨
// 적었는지 기계적으로 다시 검증한다. 위반 시 IllegalArgumentException을 던져, 각 JobProcessor의 기존
// 재시도 분류(21.2절: 스키마 실패와 동일하게 제한된 재시도 후 FAILED)를 그대로 탄다.
public final class SummaryOriginalityValidator {

	// 공백을 제거한 뒤에도 이만큼 길게 원문과 연속으로 겹치면 "다듬어 표현"이 아니라 "그대로 베낀" 것으로 본다.
	private static final int MIN_VERBATIM_MATCH_LENGTH = 25;

	// 겹치는 구간이 아무리 길어도 summary 자체가 훨씬 길면(=베낀 부분 뒤에 실제 정리가 이어지면) 위반으로
	// 보지 않는다. 베낀 구간이 summary 전체의 상당 부분을 차지할 때만 규칙 위반으로 판단한다.
	private static final double MIN_VERBATIM_MATCH_RATIO = 0.6;

	public static void validate(String contextContent, String summary) {
		String normalizedContext = normalize(contextContent);
		String normalizedSummary = normalize(summary);
		if (normalizedContext.isEmpty() || normalizedSummary.isEmpty()) {
			return;
		}

		int longestMatch = longestCommonSubstringLength(normalizedContext, normalizedSummary);
		boolean isMostlyVerbatim = longestMatch >= MIN_VERBATIM_MATCH_LENGTH
				&& longestMatch >= normalizedSummary.length() * MIN_VERBATIM_MATCH_RATIO;
		if (isMostlyVerbatim) {
			throw new IllegalArgumentException("AI 응답의 summary가 원문을 그대로 옮겨 적은 것으로 판단됩니다");
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.replaceAll("\\s+", "");
	}

	// 두 문자열 사이 가장 긴 연속 공통 부분 문자열의 길이. summary(최대 수백 자)·contextContent(최대
	// 수천 자) 크기에서 O(n*m) 동적 계획법으로 충분히 빠르다.
	private static int longestCommonSubstringLength(String a, String b) {
		int[] previousRow = new int[b.length() + 1];
		int[] currentRow = new int[b.length() + 1];
		int longest = 0;
		for (int i = 1; i <= a.length(); i++) {
			for (int j = 1; j <= b.length(); j++) {
				if (a.charAt(i - 1) == b.charAt(j - 1)) {
					currentRow[j] = previousRow[j - 1] + 1;
					longest = Math.max(longest, currentRow[j]);
				} else {
					currentRow[j] = 0;
				}
			}
			int[] swap = previousRow;
			previousRow = currentRow;
			currentRow = swap;
		}
		return longest;
	}

	private SummaryOriginalityValidator() {
	}

}
