package com.naroom.api.appcontent.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum AppContentErrorCode implements ErrorCode {

	ITEM_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"APP_CONTENT_ITEM_NOT_FOUND",
			"urn:naroom:problem:app-content-item-not-found",
			"문구를 찾을 수 없습니다",
			"존재하지 않는 문구입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ITEM_KEY_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"APP_CONTENT_ITEM_KEY_ALREADY_EXISTS",
			"urn:naroom:problem:app-content-item-key-already-exists",
			"이미 사용 중인 키입니다",
			"같은 contentKey·locale 조합이 이미 등록되어 있습니다. 다른 contentKey를 사용하거나 기존 문구를 수정해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ITEM_NOT_DRAFT(
			HttpStatus.CONFLICT,
			"APP_CONTENT_ITEM_NOT_DRAFT",
			"urn:naroom:problem:app-content-item-not-draft",
			"수정할 수 없는 문구입니다",
			"DRAFT 상태의 문구만 내용을 수정할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ITEM_NOT_PUBLISHED(
			HttpStatus.CONFLICT,
			"APP_CONTENT_ITEM_NOT_PUBLISHED",
			"urn:naroom:problem:app-content-item-not-published",
			"처리할 수 없는 문구입니다",
			"PUBLISHED 상태의 문구에서만 새 리비전을 만들거나 보관할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	VALUE_TYPE_MISMATCH(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"APP_CONTENT_VALUE_TYPE_MISMATCH",
			"urn:naroom:problem:app-content-value-type-mismatch",
			"요청 내용을 확인해 주세요",
			"valueType=TEXT면 valueText만, valueType=JSON이면 valueJson만 채워야 합니다.",
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

	AppContentErrorCode(
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
