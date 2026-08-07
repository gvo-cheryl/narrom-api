package com.naroom.api.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// naroom.notification.dispatch.enabled가 true일 때만 활성화된다(AiJobClaimScheduler와 같은 opt-in 패턴).
@Component
@ConditionalOnProperty(prefix = "naroom.notification.dispatch", name = "enabled", havingValue = "true")
public class NotificationDispatchScheduler {

	private static final Logger log = LoggerFactory.getLogger(NotificationDispatchScheduler.class);

	private final NotificationDispatchService notificationDispatchService;

	public NotificationDispatchScheduler(NotificationDispatchService notificationDispatchService) {
		this.notificationDispatchService = notificationDispatchService;
	}

	@Scheduled(fixedDelayString = "${naroom.notification.dispatch.poll-interval}")
	public void dispatchDue() {
		int sent = notificationDispatchService.dispatchDue();
		if (sent > 0) {
			log.info("dispatched {} notifications", sent);
		}
	}

}
