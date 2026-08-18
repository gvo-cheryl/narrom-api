package com.naroom.api.auth.apple;

import com.naroom.api.account.domain.entity.SocialProvider;
import com.naroom.api.auth.social.SocialCredential;
import com.naroom.api.auth.social.SocialProviderClient;
import com.naroom.api.auth.social.SocialUserInfo;
import org.springframework.stereotype.Component;

@Component
public class AppleSocialProviderClient implements SocialProviderClient {

	private final AppleClient appleClient;

	public AppleSocialProviderClient(AppleClient appleClient) {
		this.appleClient = appleClient;
	}

	@Override
	public SocialProvider provider() {
		return SocialProvider.APPLE;
	}

	@Override
	public SocialUserInfo fetchUserInfo(SocialCredential credential) {
		AppleUserInfo appleUser = appleClient.verify(credential.token(), credential.rawNonce());
		return new SocialUserInfo(
				appleUser.sub(),
				appleUser.email(),
				appleUser.emailVerified(),
				credential.fullNameHint(),
				null);
	}

}
