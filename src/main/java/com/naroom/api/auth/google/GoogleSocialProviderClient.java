package com.naroom.api.auth.google;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.social.SocialProviderClient;
import com.naroom.api.auth.social.SocialUserInfo;
import org.springframework.stereotype.Component;

@Component
public class GoogleSocialProviderClient implements SocialProviderClient {

	private final GoogleClient googleClient;

	public GoogleSocialProviderClient(GoogleClient googleClient) {
		this.googleClient = googleClient;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.GOOGLE;
	}

	@Override
	public SocialUserInfo fetchUserInfo(String credential) {
		GoogleUserInfo googleUser = googleClient.verify(credential);
		return new SocialUserInfo(
				googleUser.sub(),
				googleUser.email(),
				googleUser.emailVerified(),
				googleUser.name(),
				googleUser.picture());
	}

}
