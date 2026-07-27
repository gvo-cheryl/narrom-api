package com.naroom.api.ai.pipeline;

import com.naroom.api.ai.AiResponseGenerationClient;
import com.naroom.api.ai.GenerationRequest;
import com.naroom.api.ai.GenerationResult;

// 실제 API를 부르지 않는 테스트 전용 Fake. generate()를 실제로 호출했는지(callCount)와 마지막 요청 내용을
// 검증에 쓸 수 있게 노출한다.
public class FakeAiResponseGenerationClient implements AiResponseGenerationClient {

	private final String outputJson;
	private final RuntimeException failure;
	private GenerationRequest lastRequest;
	private int callCount;

	public FakeAiResponseGenerationClient(String outputJson) {
		this.outputJson = outputJson;
		this.failure = null;
	}

	public FakeAiResponseGenerationClient(RuntimeException failure) {
		this.outputJson = null;
		this.failure = failure;
	}

	@Override
	public GenerationResult generate(GenerationRequest request) {
		callCount++;
		lastRequest = request;
		if (failure != null) {
			throw failure;
		}
		return new GenerationResult(outputJson, 120, 40);
	}

	public GenerationRequest lastRequest() {
		return lastRequest;
	}

	public int callCount() {
		return callCount;
	}

}
