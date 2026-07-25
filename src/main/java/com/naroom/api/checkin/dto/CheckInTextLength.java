package com.naroom.api.checkin.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CheckInTextLengthValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckInTextLength {

	String message() default "체크인 문장형 입력의 합산 글자 수가 상한을 초과했습니다.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	int max() default 1000;
}
