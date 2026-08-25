package com.naroom.api.admin.common;

import com.naroom.api.global.error.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSortParserTest {

	private static final Set<String> ALLOWED = Set.of("updatedAt", "code");
	private static final Sort FALLBACK = Sort.by(Sort.Direction.ASC, "code");

	@Test
	void blankSort_returnsFallback() {
		assertThat(AdminSortParser.parse(null, ALLOWED, FALLBACK)).isEqualTo(FALLBACK);
		assertThat(AdminSortParser.parse("  ", ALLOWED, FALLBACK)).isEqualTo(FALLBACK);
	}

	@Test
	void fieldOnly_defaultsToAscending() {
		Sort sort = AdminSortParser.parse("updatedAt", ALLOWED, FALLBACK);
		assertThat(sort.getOrderFor("updatedAt").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void fieldWithDesc_parsesDirection() {
		Sort sort = AdminSortParser.parse("updatedAt,desc", ALLOWED, FALLBACK);
		assertThat(sort.getOrderFor("updatedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void fieldNotAllowed_throwsBusinessException() {
		assertThatThrownBy(() -> AdminSortParser.parse("email,desc", ALLOWED, FALLBACK))
				.isInstanceOf(BusinessException.class);
	}

}
