package com.naroom.api.lifetime.dto;

import com.naroom.api.ai.domain.entity.AiFeatureType;
import jakarta.validation.constraints.NotNull;

public record PeriodReflectionCreateRequest(@NotNull AiFeatureType featureType) {
}
