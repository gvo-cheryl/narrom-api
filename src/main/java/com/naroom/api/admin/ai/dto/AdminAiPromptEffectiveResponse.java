package com.naroom.api.admin.ai.dto;

import com.naroom.api.ai.prompt.AiPromptResolver;

// 지금 실제로 쓰이고 있는 지침 내용 - 관리자가 발행한 게 있으면 그 내용, 없으면 AiInstructionCatalog(코드)
// 기본값이다. 새 초안을 작성할 때 빈 화면 대신 이 값을 미리 채워주는 용도로만 쓴다.
public record AdminAiPromptEffectiveResponse(
		String versionLabel, String content, String modelName, Integer outputMaxLength, boolean fromAdminContent) {

	public static AdminAiPromptEffectiveResponse from(AiPromptResolver.ResolvedCommonInstructions resolved) {
		return new AdminAiPromptEffectiveResponse(
				resolved.versionLabel(), resolved.content(), null, null, resolved.fromAdminContent());
	}

	public static AdminAiPromptEffectiveResponse from(AiPromptResolver.ResolvedFeatureInstructions resolved) {
		return new AdminAiPromptEffectiveResponse(
				resolved.versionLabel(), resolved.content(), resolved.modelName(), resolved.outputMaxLength(),
				resolved.fromAdminContent());
	}

}
