package com.naroom.api.lifetime.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum LifetimeErrorCode implements ErrorCode {

	PERIOD_REFLECTION_INSUFFICIENT_RECORDS(
			HttpStatus.BAD_REQUEST,
			"LIFETIME_PERIOD_REFLECTION_INSUFFICIENT_RECORDS",
			"urn:naroom:problem:lifetime-period-reflection-insufficient-records",
			"기록이 부족합니다",
			"이 기간에 남긴 기록이 너무 적어 회고를 만들 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PERIOD_REFLECTION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"LIFETIME_PERIOD_REFLECTION_NOT_FOUND",
			"urn:naroom:problem:lifetime-period-reflection-not-found",
			"기간별 회고를 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 기간별 회고입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PERIOD_REFLECTION_FEATURE_TYPE_INVALID(
			HttpStatus.BAD_REQUEST,
			"LIFETIME_PERIOD_REFLECTION_FEATURE_TYPE_INVALID",
			"urn:naroom:problem:lifetime-period-reflection-feature-type-invalid",
			"요청 내용을 확인해 주세요",
			"featureType은 THREE_DAY_REFLECTION 또는 WEEKLY_REFLECTION만 가능합니다.",
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

	LifetimeErrorCode(
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
