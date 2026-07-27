package com.naroom.api.ai;

// outputJson은 요청한 JSON Schema를 따르는 원문 텍스트다. 실제 타입으로 파싱·검증하는 것은 4-G의 몫이다.
public record GenerationResult(String outputJson, long inputTokens, long outputTokens) {
}
