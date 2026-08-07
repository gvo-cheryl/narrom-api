package com.naroom.api.notification.infra.expo;

import java.util.Map;

// https://docs.expo.dev/push-notifications/sending-notifications/#message-request-format
public record ExpoPushMessage(String to, String title, String body, Map<String, String> data) {
}
