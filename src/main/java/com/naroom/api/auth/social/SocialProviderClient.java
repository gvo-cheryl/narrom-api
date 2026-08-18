package com.naroom.api.auth.social;

import com.naroom.api.account.domain.entity.SocialProvider;

// provider별 자격 증명(카카오 provider access token, Google/Apple ID Token 등)을 검증하고
// SocialUserInfo로 정규화하는 어댑터. 검증 실패 시 BusinessException(AuthErrorCode)을 던진다.
public interface SocialProviderClient {

	SocialProvider provider();

	SocialUserInfo fetchUserInfo(String credential);

}
