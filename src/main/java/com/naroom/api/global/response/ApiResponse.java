package com.naroom.api.global.response;

// conventions.md 성공 응답 규칙: 단건·목록 모두 {"data": ...}로 감싼다. 커서 목록은 CursorPageResponse를 대신 쓴다.
public record ApiResponse<T>(T data) {

	public static <T> ApiResponse<T> of(T data) {
		return new ApiResponse<>(data);
	}

}
