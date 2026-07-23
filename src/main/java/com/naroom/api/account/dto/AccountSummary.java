package com.naroom.api.account.dto;

import com.naroom.api.account.domain.entity.MemberStatus;

import java.time.Instant;
import java.util.UUID;

// 로그인·재발급·온보딩 완료 응답이 공통으로 쓰는 회원 요약 정보.
public record AccountSummary(UUID memberId, MemberStatus status, Instant onboardingCompletedAt, Long version) {
}
