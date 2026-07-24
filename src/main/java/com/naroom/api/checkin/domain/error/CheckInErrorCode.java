package com.naroom.api.checkin.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

// 특정 날짜에 체크인이 없는 것은 오류가 아니라 "아직 안 함" 상태라 별도 NOT_FOUND 코드를 두지 않는다
// (CheckInService는 Optional로, Controller는 data=null로 표현한다).
public enum CheckInErrorCode implements ErrorCode {

	CHECK_IN_EMOTION_TAG_INVALID(
			HttpStatus.BAD_REQUEST,
			"CHECKIN_EMOTION_TAG_INVALID",
			"urn:naroom:problem:checkin-emotion-tag-invalid",
			"요청 내용을 확인해 주세요",
			"감정(EMOTION) 분류 태그만 체크인에 선택할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false);

	private final HttpStatus httpStatus;
	private final String code;
	private final String type;
	private final String title;
	private final String detail;
	private final ErrorStage stage;
	private final ClientAction action;
	private final boolean retryable;

	CheckInErrorCode(
			HttpStatus httpStatus,
			String code,
			String type,
			String title,
			String detail,
			ErrorStage stage,
			ClientAction action,
			boolean retryable) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.type = type;
		this.title = title;
		this.detail = detail;
		this.stage = stage;
		this.action = action;
		this.retryable = retryable;
	}

	@Override
	public HttpStatus httpStatus() {
		return httpStatus;
	}

	@Override
	public String code() {
		return code;
	}

	@Override
	public String type() {
		return type;
	}

	@Override
	public String title() {
		return title;
	}

	@Override
	public String detail() {
		return detail;
	}

	@Override
	public ErrorStage stage() {
		return stage;
	}

	@Override
	public ClientAction action() {
		return action;
	}

	@Override
	public boolean retryable() {
		return retryable;
	}

}
