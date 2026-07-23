package com.naroom.api.global.response;

import java.util.List;

public record CursorPageResponse<T>(List<T> data, PageInfo page) {

	public static <T> CursorPageResponse<T> of(List<T> data, PageInfo page) {
		return new CursorPageResponse<>(data, page);
	}

}
