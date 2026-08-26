package com.naroom.api.admin.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminAiPromptCreateRevisionRequest(@NotBlank String versionLabel) {
}
