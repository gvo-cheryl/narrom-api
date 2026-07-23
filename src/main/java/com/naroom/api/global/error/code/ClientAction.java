package com.naroom.api.global.error.code;

// docs/api/error-response.md의 action 어휘와 반드시 동기화한다. 여기서 값을 늘리거나 바꾸면 문서도 같이 고친다.
public enum ClientAction {
	NONE,
	RETRY,
	CHECK_REQUEST,
	CHECK_DEVICE,
	LOGIN_REQUIRED,
	REFRESH_REQUIRED,
	CLEAR_SESSION_AND_LOGIN,
	COMPLETE_ONBOARDING,
	CONFIRM_ACCOUNT_RECOVERY,
	RELOAD_RESOURCE,
	CONTACT_SUPPORT
}
