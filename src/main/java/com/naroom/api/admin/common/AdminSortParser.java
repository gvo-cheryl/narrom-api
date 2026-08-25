package com.naroom.api.admin.common;

import com.naroom.api.global.error.code.CommonErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.data.domain.Sort;

import java.util.Set;

// docs/contracts/drafts/admin-list-search-sort.md 권장안: "field,asc|desc" 형식 한 개만 허용한다.
// 카테고리별 allowlist에 없는 field는 임의 프로퍼티 접근(HQL 예외·의도치 않은 컬럼 노출)을 막기 위해 거부한다.
public final class AdminSortParser {

	private AdminSortParser() {
	}

	public static Sort parse(String sort, Set<String> allowedFields, Sort fallback) {
		if (sort == null || sort.isBlank()) {
			return fallback;
		}
		String[] parts = sort.split(",", 2);
		String field = parts[0].trim();
		if (!allowedFields.contains(field)) {
			throw new BusinessException(CommonErrorCode.VALIDATION_FAILED);
		}
		Sort.Direction direction =
				parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, field);
	}

}
