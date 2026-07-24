package com.naroom.api.content.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum ContentErrorCode implements ErrorCode {

	QUOTE_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"CONTENT_QUOTE_NOT_FOUND",
			"urn:naroom:problem:content-quote-not-found",
			"문장을 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 문장입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	QUOTE_TOPIC_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"CONTENT_QUOTE_TOPIC_NOT_FOUND",
			"urn:naroom:problem:content-quote-topic-not-found",
			"주제를 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 주제입니다.",
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

	ContentErrorCode(
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
