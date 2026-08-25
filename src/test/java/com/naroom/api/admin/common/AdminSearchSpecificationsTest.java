package com.naroom.api.admin.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSearchSpecificationsTest {

	private static final List<String> FIELDS = List.of("code", "text");

	// Spring Data JPA 4.x의 Specification.where()는 null을 인자로 받으면 예외를 던진다 - 검색어가 없을 때
	// null을 반환하면 각 서비스의 list()가 전부 500으로 깨진다(실제로 겪은 회귀). unrestricted()로 대체했는지 확인한다.
	@Test
	void blankQ_returnsUnrestrictedSpecification_notNull() {
		Specification<Object> specification = AdminSearchSpecifications.containsAnyIgnoreCase(null, FIELDS);

		assertThat(specification).isNotNull();
		assertThat(Specification.where(specification)).isNotNull();
	}

	@Test
	void nonBlankQ_returnsNonNullSpecification() {
		Specification<Object> specification = AdminSearchSpecifications.containsAnyIgnoreCase("hello", FIELDS);

		assertThat(specification).isNotNull();
	}

}
