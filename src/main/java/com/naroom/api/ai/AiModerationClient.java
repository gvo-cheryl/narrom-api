package com.naroom.api.ai;

import com.naroom.api.ai.domain.entity.AiSafetyGrade;

// SDK 타입은 인프라 계층(ai.infra.openai)에만 두고, 서비스 계층은 이 순수 인터페이스만 본다.
public interface AiModerationClient {

	AiSafetyGrade classify(String text);

}
