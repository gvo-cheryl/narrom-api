package com.naroom.api.auth.apple;

// Apple identity token claim 중 로그인에 필요한 값만 정규화한 것. email은 Apple 비공개 릴레이 주소일 수
// 있으나 일반 이메일과 동일하게 다룬다. 이름은 identity token에 없어 여기 포함되지 않는다(요청의 fullName
// 값으로 별도 처리).
public record AppleUserInfo(
		String sub,
		String email,
		boolean emailVerified) {
}
