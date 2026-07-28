package com.naroom.api.lifetime.dto;

import com.naroom.api.record.domain.entity.TagCategory;

import java.util.UUID;

// count는 순위·점수가 아니라 단순 등장 횟수다(단정·비교 금지 원칙과는 별개 - 사용자 자신의 태그 빈도 탐색 목적).
public record TagDistributionResponse(UUID tagId, String tagName, TagCategory category, long count) {
}
