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
			false),

	REFLECTION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_REFLECTION_NOT_FOUND",
			"urn:naroom:problem:ai-reflection-not-found",
			"AI 정리를 찾을 수 없습니다",
			"존재하지 않거나, 이 기록의 것이 아니거나, 아직 완료되지 않은 AI 정리입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	GENERATION_RUN_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_GENERATION_RUN_NOT_FOUND",
			"urn:naroom:problem:ai-generation-run-not-found",
			"AI 생성 이력을 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 AI 생성 이력입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	FEEDBACK_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_FEEDBACK_NOT_FOUND",
			"urn:naroom:problem:ai-feedback-not-found",
			"AI 만족도 평가를 찾을 수 없습니다",
			"이 AI 생성 이력에 대한 평가가 아직 없습니다. 먼저 평가를 제출해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_VERSION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"AI_PROMPT_VERSION_NOT_FOUND",
			"urn:naroom:problem:ai-prompt-version-not-found",
			"프롬프트 버전을 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 프롬프트 버전입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_VERSION_NOT_DRAFT(
			HttpStatus.CONFLICT,
			"AI_PROMPT_VERSION_NOT_DRAFT",
			"urn:naroom:problem:ai-prompt-version-not-draft",
			"수정할 수 없는 상태입니다",
			"초안(DRAFT) 상태의 프롬프트 버전만 수정하거나 발행할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_VERSION_NOT_PUBLISHED(
			HttpStatus.CONFLICT,
			"AI_PROMPT_VERSION_NOT_PUBLISHED",
			"urn:naroom:problem:ai-prompt-version-not-published",
			"처리할 수 없는 상태입니다",
			"발행(PUBLISHED) 상태의 프롬프트 버전에만 적용할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_VERSION_LABEL_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"AI_PROMPT_VERSION_LABEL_ALREADY_EXISTS",
			"urn:naroom:problem:ai-prompt-version-label-already-exists",
			"이미 사용 중인 버전 라벨입니다",
			"같은 범위(공통 또는 같은 기능)에서 다른 라벨을 입력해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	FEATURE_TYPE_NOT_EDITABLE(
			HttpStatus.CONFLICT,
			"AI_FEATURE_TYPE_NOT_EDITABLE",
			"urn:naroom:problem:ai-feature-type-not-editable",
			"아직 편집할 수 없는 기능입니다",
			"이 기능은 아직 구현된 프롬프트가 없어 관리자 편집 대상이 아닙니다.",
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
