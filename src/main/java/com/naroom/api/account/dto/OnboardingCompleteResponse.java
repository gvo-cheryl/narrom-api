package com.naroom.api.account.dto;

import com.naroom.api.auth.NextAction;

// authentication.md 온보딩 완료 성공 응답과 1:1로 대응한다.
public record OnboardingCompleteResponse(AccountSummary account, NextAction nextAction) {
}
