package com.naroom.api.global.error.code;

// docs/api/error-response.md의 stage 어휘와 반드시 동기화한다. 여기서 값을 늘리거나 바꾸면 문서도 같이 고친다.
public enum ErrorStage {
	REQUEST,
	DEVICE,
	LOGIN,
	TOKEN,
	SESSION,
	ACCOUNT,
	ONBOARDING,
	PERSISTENCE,
	EXTERNAL,
	INTERNAL
}
