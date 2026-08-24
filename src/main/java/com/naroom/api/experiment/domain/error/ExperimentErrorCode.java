package com.naroom.api.experiment.domain.error;

import com.naroom.api.global.error.code.ClientAction;
import com.naroom.api.global.error.code.ErrorCode;
import com.naroom.api.global.error.code.ErrorStage;
import org.springframework.http.HttpStatus;

public enum ExperimentErrorCode implements ErrorCode {

	PROGRAM_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"EXPERIMENT_PROGRAM_NOT_FOUND",
			"urn:naroom:problem:experiment-program-not-found",
			"코스를 찾을 수 없습니다",
			"존재하지 않거나 아직 공개되지 않은 코스입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	DURATION_INVALID(
			HttpStatus.BAD_REQUEST,
			"EXPERIMENT_DURATION_INVALID",
			"urn:naroom:problem:experiment-duration-invalid",
			"요청 내용을 확인해 주세요",
			"days는 3 또는 7만 가능합니다(Beta 1 노출 범위).",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ACTIVE_PROGRAM_EXISTS(
			HttpStatus.CONFLICT,
			"EXPERIMENT_ACTIVE_PROGRAM_EXISTS",
			"urn:naroom:problem:experiment-active-program-exists",
			"이미 진행 중인 코스가 있습니다",
			"진행하던 코스를 이어가거나 마무리한 뒤에 새 코스를 시작할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	USER_PROGRAM_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"EXPERIMENT_USER_PROGRAM_NOT_FOUND",
			"urn:naroom:problem:experiment-user-program-not-found",
			"코스를 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 코스입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	USER_PROGRAM_NOT_READY(
			HttpStatus.CONFLICT,
			"EXPERIMENT_USER_PROGRAM_NOT_READY",
			"urn:naroom:problem:experiment-user-program-not-ready",
			"시작할 수 없는 코스입니다",
			"저장된 상태의 코스만 지금 시작할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	MISSION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"EXPERIMENT_MISSION_NOT_FOUND",
			"urn:naroom:problem:experiment-mission-not-found",
			"미션을 찾을 수 없습니다",
			"존재하지 않거나 더 이상 사용하지 않는 미션입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	INVALID_MISSION_COUNT(
			HttpStatus.BAD_REQUEST,
			"EXPERIMENT_INVALID_MISSION_COUNT",
			"urn:naroom:problem:experiment-invalid-mission-count",
			"요청 내용을 확인해 주세요",
			"코스 기간(durationDays)만큼 서로 다른 미션이 필요합니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	INVALID_DAY_NUMBER(
			HttpStatus.BAD_REQUEST,
			"EXPERIMENT_INVALID_DAY_NUMBER",
			"urn:naroom:problem:experiment-invalid-day-number",
			"요청 내용을 확인해 주세요",
			"dayNumber는 1부터 코스 기간까지 중복 없이 하나씩 있어야 합니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	DUPLICATE_MISSION_SELECTION(
			HttpStatus.BAD_REQUEST,
			"EXPERIMENT_DUPLICATE_MISSION_SELECTION",
			"urn:naroom:problem:experiment-duplicate-mission-selection",
			"요청 내용을 확인해 주세요",
			"같은 코스 안에서는 같은 미션을 두 번 이상 고를 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	CONTENT_VERSION_MISMATCH(
			HttpStatus.CONFLICT,
			"EXPERIMENT_CONTENT_VERSION_MISMATCH",
			"urn:naroom:problem:experiment-content-version-mismatch",
			"코스 내용이 업데이트되었습니다",
			"코스 상세를 다시 불러온 뒤 다시 시도해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	USER_PROGRAM_NOT_IN_PROGRESS(
			HttpStatus.CONFLICT,
			"EXPERIMENT_USER_PROGRAM_NOT_IN_PROGRESS",
			"urn:naroom:problem:experiment-user-program-not-in-progress",
			"지금은 기록할 수 없는 코스입니다",
			"진행 중인 코스에서만 오늘의 작은 실험을 기록할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	MISSION_NOT_CURRENT(
			HttpStatus.CONFLICT,
			"EXPERIMENT_MISSION_NOT_CURRENT",
			"urn:naroom:problem:experiment-mission-not-current",
			"오늘의 미션이 아닙니다",
			"현재 진행 중인 일차의 미션만 기록할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	MISSION_ALREADY_RECORDED(
			HttpStatus.CONFLICT,
			"EXPERIMENT_MISSION_ALREADY_RECORDED",
			"urn:naroom:problem:experiment-mission-already-recorded",
			"이미 기록을 마친 미션입니다",
			"이미 기록되었거나 지난 일차의 미션은 다시 기록하거나 교체할 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	ALREADY_RESTED_TODAY(
			HttpStatus.CONFLICT,
			"EXPERIMENT_ALREADY_RESTED_TODAY",
			"urn:naroom:problem:experiment-already-rested-today",
			"오늘은 이미 쉬기로 기록했습니다",
			"같은 날짜에는 오늘은 쉬기를 한 번만 기록할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	MISSION_INACTIVE(
			HttpStatus.BAD_REQUEST,
			"EXPERIMENT_MISSION_INACTIVE",
			"urn:naroom:problem:experiment-mission-inactive",
			"지금은 고를 수 없는 미션입니다",
			"더 이상 사용하지 않는 미션으로는 교체할 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	USER_PROGRAM_NOT_AWAITING_REVIEW(
			HttpStatus.CONFLICT,
			"EXPERIMENT_USER_PROGRAM_NOT_AWAITING_REVIEW",
			"urn:naroom:problem:experiment-user-program-not-awaiting-review",
			"지금은 돌아볼 수 없는 코스입니다",
			"모든 일차를 기록한 코스만 돌아보기를 저장할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	USER_PROGRAM_ALREADY_ENDED(
			HttpStatus.CONFLICT,
			"EXPERIMENT_USER_PROGRAM_ALREADY_ENDED",
			"urn:naroom:problem:experiment-user-program-already-ended",
			"이미 끝난 코스입니다",
			"이미 완료되었거나 중단된 코스는 다시 마무리할 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	RECOMMENDATION_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"EXPERIMENT_RECOMMENDATION_NOT_FOUND",
			"urn:naroom:problem:experiment-recommendation-not-found",
			"추천을 찾을 수 없습니다",
			"존재하지 않거나 다른 회원의 추천입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	RECOMMENDATION_NOT_ACTIONABLE(
			HttpStatus.CONFLICT,
			"EXPERIMENT_RECOMMENDATION_NOT_ACTIONABLE",
			"urn:naroom:problem:experiment-recommendation-not-actionable",
			"이미 처리된 추천입니다",
			"이미 수락했거나 넘긴 추천은 다시 살펴보거나 넘길 수 없습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	TOPIC_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"EXPERIMENT_TOPIC_NOT_FOUND",
			"urn:naroom:problem:experiment-topic-not-found",
			"주제를 찾을 수 없습니다",
			"존재하지 않는 주제입니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	TOPIC_CODE_DUPLICATE(
			HttpStatus.CONFLICT,
			"EXPERIMENT_TOPIC_CODE_DUPLICATE",
			"urn:naroom:problem:experiment-topic-code-duplicate",
			"이미 사용 중인 코드입니다",
			"다른 code 값을 사용해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	MISSION_CODE_DUPLICATE(
			HttpStatus.CONFLICT,
			"EXPERIMENT_MISSION_CODE_DUPLICATE",
			"urn:naroom:problem:experiment-mission-code-duplicate",
			"이미 사용 중인 코드입니다",
			"다른 code 값을 사용해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROGRAM_CODE_DUPLICATE(
			HttpStatus.CONFLICT,
			"EXPERIMENT_PROGRAM_CODE_DUPLICATE",
			"urn:naroom:problem:experiment-program-code-duplicate",
			"이미 사용 중인 코드입니다",
			"다른 code 값을 사용해 주세요.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROGRAM_NOT_DRAFT(
			HttpStatus.CONFLICT,
			"EXPERIMENT_PROGRAM_NOT_DRAFT",
			"urn:naroom:problem:experiment-program-not-draft",
			"수정할 수 없는 코스입니다",
			"DRAFT 상태의 코스만 내용을 수정할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROGRAM_NOT_PUBLISHED(
			HttpStatus.CONFLICT,
			"EXPERIMENT_PROGRAM_NOT_PUBLISHED",
			"urn:naroom:problem:experiment-program-not-published",
			"처리할 수 없는 코스입니다",
			"PUBLISHED 상태의 코스에서만 새 리비전을 만들거나 보관할 수 있습니다.",
			ErrorStage.REQUEST,
			ClientAction.CHECK_REQUEST,
			false),

	PROGRAM_REPLACEMENT_GROUP_INVALID(
			HttpStatus.UNPROCESSABLE_ENTITY,
			"EXPERIMENT_PROGRAM_REPLACEMENT_GROUP_INVALID",
			"urn:naroom:problem:experiment-program-replacement-group-invalid",
			"게시할 수 없는 코스입니다",
			"교체 가능으로 표시된 미션은 replacementGroup이 있어야 하고, 같은 그룹 안에 대체 후보가 1개 이상 있어야 합니다.",
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

	ExperimentErrorCode(
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
