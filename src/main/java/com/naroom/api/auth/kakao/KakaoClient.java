package com.naroom.api.auth.kakao;

import com.naroom.api.auth.domain.error.AuthErrorCode;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

// 카카오 Provider Access Token 검증은 클라이언트가 이미 받은 토큰을 이 엔드포인트로 조회하는 방식이라
// Kakao REST API 키/Client Secret이 필요 없다.
@Component
public class KakaoClient {

	private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

	private final RestClient restClient = RestClient.create();

	public KakaoUserInfoResponse fetchUserInfo(String providerAccessToken) {
		try {
			return restClient.get()
					.uri(USER_INFO_URI)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken)
					.retrieve()
					.body(KakaoUserInfoResponse.class);
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().is4xxClientError()) {
				throw new BusinessException(AuthErrorCode.AUTH_KAKAO_TOKEN_INVALID);
			}
			throw new BusinessException(AuthErrorCode.AUTH_KAKAO_UNAVAILABLE);
		} catch (RestClientException ex) {
			throw new BusinessException(AuthErrorCode.AUTH_KAKAO_UNAVAILABLE);
		}
	}

}
