package com.naroom.api.ai.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum AiErrorCode implements ErrorCode {

	JOB_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_JOB_NOT_FOUND",
			"urn:naroom:problem:ai-job-not-found",
			"AI 작업을 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 AI 작업입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	CONVERSATION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_CONVERSATION_NOT_FOUND",
			"urn:naroom:problem:ai-conversation-not-found",
			"대화를 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 대화입니다.",
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

	AiErrorCode(
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
