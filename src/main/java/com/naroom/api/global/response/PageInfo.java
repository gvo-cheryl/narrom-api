package com.naroom.api.global.response;

// nextCursor는 클라이언트가 해석하지 않는 opaque 문자열이다(conventions.md).
public record PageInfo(String nextCursor, boolean hasNext) {
}
