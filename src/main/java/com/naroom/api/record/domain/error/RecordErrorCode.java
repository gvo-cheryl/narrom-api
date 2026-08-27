package com.naroom.api.record.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum RecordErrorCode implements ErrorCode {

	ENTRY_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"RECORD_ENTRY_NOT_FOUND",
			"urn:naroom:problem:record-entry-not-found",
			"기록을 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 기록입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ENTRY_VERSION_CONFLICT(
			HttpStatus.CONFLICT,
			"RECORD_ENTRY_VERSION_CONFLICT",
			"urn:naroom:problem:record-entry-version-conflict",
			"기록이 변경되었습니다",
			"최신 내용을 다시 불러온 뒤 수정해 주세요.",
			ErrorStage.PERSISTENCE,
			ClientAction.RELOAD_RESOURCE,
			false),

	ENTRY_TYPE_QUOTE_MISMATCH(
			HttpStatus.BAD_REQUEST,
			"RECORD_ENTRY_TYPE_QUOTE_MISMATCH",
			"urn:naroom:problem:record-entry-type-quote-mismatch",
			"요청 내용을 확인해 주세요",
			"문장 기반 기록(QUOTE_REFLECTION)만 문장을 연결할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ENTRY_TYPE_NOT_USER_CREATABLE(
			HttpStatus.BAD_REQUEST,
			"RECORD_ENTRY_TYPE_NOT_USER_CREATABLE",
			"urn:naroom:problem:record-entry-type-not-user-creatable",
			"요청 내용을 확인해 주세요",
			"이 기록 유형은 해당 기능(체크인·실험·주간회고 등)을 통해서만 만들 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ENTRY_BODY_TOO_LONG(
			HttpStatus.BAD_REQUEST,
			"RECORD_ENTRY_BODY_TOO_LONG",
			"urn:naroom:problem:record-entry-body-too-long",
			"기록 내용이 너무 깁니다",
			"허용된 글자 수를 넘었습니다. context.maxLength를 확인해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	TAG_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"RECORD_TAG_NOT_FOUND",
			"urn:naroom:problem:record-tag-not-found",
			"태그를 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 태그입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ENTRY_TAG_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"RECORD_ENTRY_TAG_NOT_FOUND",
			"urn:naroom:problem:record-entry-tag-not-found",
			"연결된 태그를 찾을 수 없습니다",
			"이미 처리되었거나 존재하지 않는 태그 연결입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	SELF_REFLECTION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"RECORD_SELF_REFLECTION_NOT_FOUND",
			"urn:naroom:problem:record-self-reflection-not-found",
			"기록을 찾을 수 없습니다",
			"삭제되었거나 존재하지 않는 자기회고입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	CALENDAR_MONTH_INVALID(
			HttpStatus.BAD_REQUEST,
			"RECORD_CALENDAR_MONTH_INVALID",
			"urn:naroom:problem:record-calendar-month-invalid",
			"요청 내용을 확인해 주세요",
			"month는 1에서 12 사이여야 합니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"RECORD_PROMPT_NOT_FOUND",
			"urn:naroom:problem:record-prompt-not-found",
			"질문을 찾을 수 없습니다",
			"존재하지 않는 질문 버전입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_CODE_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"RECORD_PROMPT_CODE_ALREADY_EXISTS",
			"urn:naroom:problem:record-prompt-code-already-exists",
			"이미 사용 중인 코드입니다",
			"기존 질문을 수정하려면 새 버전(revision)을 만들어 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_NOT_DRAFT(
			HttpStatus.CONFLICT,
			"RECORD_PROMPT_NOT_DRAFT",
			"urn:naroom:problem:record-prompt-not-draft",
			"수정할 수 없는 상태입니다",
			"초안(DRAFT) 상태의 질문만 수정하거나 발행할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_NOT_PUBLISHED(
			HttpStatus.CONFLICT,
			"RECORD_PROMPT_NOT_PUBLISHED",
			"urn:naroom:problem:record-prompt-not-published",
			"처리할 수 없는 상태입니다",
			"발행(PUBLISHED) 상태의 질문에만 적용할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROMPT_LAST_PUBLISHED_CANNOT_BE_ARCHIVED(
			HttpStatus.CONFLICT,
			"RECORD_PROMPT_LAST_PUBLISHED_CANNOT_BE_ARCHIVED",
			"urn:naroom:problem:record-prompt-last-published-cannot-be-archived",
			"마지막 질문은 보관할 수 없습니다",
			"발행된 질문이 0개가 되지 않도록, 새 질문을 먼저 발행한 뒤 보관해 주세요.",
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

	RecordErrorCode(
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
