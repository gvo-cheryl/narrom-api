package com.naroom.api.checkin.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckInTextLengthValidator implements ConstraintValidator<CheckInTextLength, CheckInUpsertRequest> {

	private int max;

	@Override
	public void initialize(CheckInTextLength constraintAnnotation) {
		this.max = constraintAnnotation.max();
	}

	@Override
	public boolean isValid(CheckInUpsertRequest request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		int totalLength = length(request.memorableEvent())
				+ length(request.gratitudeNote())
				+ length(request.currentNeed())
				+ length(request.freeNote());
		if (totalLength <= max) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		// GlobalExceptionHandler는 FieldError만 추출하므로, addPropertyNode로 필드 경로를
		// 지정해 이 클래스 레벨 위반도 일반 필드 검증 오류와 같은 형태로 응답되게 한다.
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode("freeNote")
				.addConstraintViolation();
		return false;
	}

	private int length(String value) {
		return value == null ? 0 : value.length();
	}
}
