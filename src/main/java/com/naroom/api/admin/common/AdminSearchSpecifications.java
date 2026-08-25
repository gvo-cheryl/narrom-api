package com.naroom.api.admin.common;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

// docs/contracts/drafts/admin-list-search-sort.md 권장안: q가 있으면 지정된 필드들에 대해 대소문자
// 무시 부분일치(LIKE) OR 조건으로 좁힌다. 관리자 목록 API 전체가 이 규정을 공유한다.
public final class AdminSearchSpecifications {

	private AdminSearchSpecifications() {
	}

	public static <T> Specification<T> containsAnyIgnoreCase(String q, List<String> fields) {
		if (q == null || q.isBlank()) {
			return null;
		}
		String pattern = "%" + q.toLowerCase() + "%";
		return (root, query, criteriaBuilder) -> {
			Predicate[] predicates = fields.stream()
					.map(field -> criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), pattern))
					.toArray(Predicate[]::new);
			return criteriaBuilder.or(predicates);
		};
	}

}
