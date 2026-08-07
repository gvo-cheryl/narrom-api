package com.naroom.api.notification.infra.expo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

// Expo Push Notification Service(DEC-01, docs/instruction/notification 설계 문서). 별도 API 키 없이
// 회원 기기의 Expo 푸시 토큰만으로 발송한다. Beta 1 규모에서는 메시지 하나당 요청 하나로 충분해
// Expo가 지원하는 배치 발송(최대 100건/요청)은 쓰지 않는다 - 물량이 늘면 그때 배치로 바꾼다.
@Component
public class ExpoPushClient {

	private static final String PUSH_SEND_URI = "https://exp.host/--/api/v2/push/send";
	private static final Logger log = LoggerFactory.getLogger(ExpoPushClient.class);

	private final RestClient restClient = RestClient.create();

	// 발송 실패가 스케줄러 전체를 막지 않도록 예외를 던지지 않고 로그만 남긴다 - 다음 tick에 다시 시도하지는
	// 않는다(§2 DEC-02: 하루 1회 제한을 먼저 마킹하기 때문). 토큰 만료 등은 이후 별도 정리 작업 대상이다.
	public void send(String pushToken, String title, String body) {
		try {
			restClient.post()
					.uri(PUSH_SEND_URI)
					.body(List.of(new ExpoPushMessage(pushToken, title, body, null)))
					.retrieve()
					.body(ExpoPushTicketResponse.class);
		} catch (RestClientResponseException ex) {
			log.warn("expo push send failed with response status={} body={}",
					ex.getStatusCode(), ex.getResponseBodyAsString());
		} catch (RestClientException ex) {
			log.warn("expo push send failed with non-response exception type={} message={}",
					ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

}
