package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminSession;

// rawToken은 여기서만 잠깐 존재한다 - 쿠키로 내려보내고 나면 원문은 어디에도 저장하지 않는다.
public record IssuedAdminSession(String rawToken, AdminSession session) {
}
