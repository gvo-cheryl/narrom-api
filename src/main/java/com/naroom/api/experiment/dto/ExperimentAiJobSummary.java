package com.naroom.api.experiment.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import com.naroom.api.ai.domain.entity.AiJobStatus;

// featureType/status는 실제로 AI 잡을 만든 경우(3일 코스)에만 채워진다. 7일 코스처럼 AI 회고를 만들지
// 않은 경우에는 note로만 이유를 알려준다.
public record ExperimentAiJobSummary(AiFeatureType featureType, AiJobStatus status, String note) {
}
