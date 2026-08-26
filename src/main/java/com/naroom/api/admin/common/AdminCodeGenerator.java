package com.naroom.api.admin.common;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

// 오늘의 문장/기록 시작 질문/작은 실험 주제·미션·코스의 code는 다른 테이블이 참조하는 안정 식별자일 뿐,
// 사용자가 의미를 담아 고를 필요가 없다 - 생성 시 서버가 항상 자동으로 부여한다.
public final class AdminCodeGenerator {

	private static final DateTimeFormatter TIMESTAMP_FORMAT =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

	private AdminCodeGenerator() {
	}

	public static String generate(String prefix) {
		String timestamp = TIMESTAMP_FORMAT.format(Instant.now());
		String random = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000, 0x100000));
		return prefix + "-" + timestamp + "-" + random;
	}

}
