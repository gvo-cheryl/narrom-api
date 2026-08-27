package com.naroom.api.admin.auth;

// AdminAuthenticationFailureHandler가 OAuth2Error.errorCode로 읽어 감사 로그에만 남긴다(브라우저 미노출).
public class AdminLoginRejectedException extends RuntimeException {

	public enum Reason {
		NOT_ALLOWLISTED("admin_not_allowlisted"),
		ACCOUNT_DISABLED("admin_account_disabled");

		private final String errorCode;

		Reason(String errorCode) {
			this.errorCode = errorCode;
		}

		public String errorCode() {
			return errorCode;
		}
	}

	private final Reason reason;
	private final String googleSub;

	public AdminLoginRejectedException(Reason reason, String googleSub) {
		super(reason.errorCode());
		this.reason = reason;
		this.googleSub = googleSub;
	}

	public Reason reason() {
		return reason;
	}

	public String googleSub() {
		return googleSub;
	}

}
