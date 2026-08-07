package com.naroom.api.notification;

import com.naroom.api.account.domain.entity.NotificationType;

// IA §17 M2 "알림 표현 원칙" - 완료를 강요하거나 압박하는 표현 대신 "살펴볼 수 있어요"처럼 초대하는
// 톤을 쓴다(권장: "오늘의 작은 실험을 살펴볼 수 있어요." / 지양: "오늘 미션을 완료하지 않았어요.").
final class NotificationCopy {

	private NotificationCopy() {
	}

	record Content(String title, String body) {
	}

	static Content forType(NotificationType type) {
		return switch (type) {
			case WEEKLY_REFLECTION -> new Content("이번 주 회고", "이번 주 기록을 천천히 돌아볼 수 있어요.");
			case EXPERIMENT_MISSION -> new Content("오늘의 작은 실험", "오늘의 작은 실험을 살펴볼 수 있어요.");
			case DAILY_QUOTE -> new Content("오늘의 문장", "오늘의 문장을 확인해볼 수 있어요.");
		};
	}

}
