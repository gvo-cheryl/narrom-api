package com.naroom.api.auth.dto;

import com.naroom.api.account.dto.AccountSummary;
import com.naroom.api.auth.NextAction;

// authentication.md 서버 세션 확인 응답과 1:1로 대응한다.
public record SessionCheckResponse(
		boolean authenticated,
		SessionSummary session,
		AccountSummary account,
		NextAction nextAction) {
}
