package com.naroom.api.admin.preview;

import com.naroom.api.admin.domain.entity.PreviewSession;

// rawToken은 여기서만 잠깐 존재한다 - 응답으로 내려보내고 나면 원문은 어디에도 저장하지 않는다.
public record IssuedPreviewSession(String rawToken, PreviewSession session) {
}
