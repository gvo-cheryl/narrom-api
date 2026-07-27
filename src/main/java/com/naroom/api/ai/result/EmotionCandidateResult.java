package com.naroom.api.ai.result;

import com.naroom.api.record.domain.entity.Tag;

import java.math.BigDecimal;

// matchedTag가 null이면 9.3절의 "unmapped emotion candidate" - 기존 표준 감정과 매칭되지 않은 표현이다.
// 공용 태그를 자동 생성하지 않으므로 이 후보는 화면에 참고 정보로만 노출하고 entry_tags에는 붙이지 않는다.
public record EmotionCandidateResult(String name, BigDecimal confidence, Tag matchedTag) {

	public boolean isMapped() {
		return matchedTag != null;
	}

}
