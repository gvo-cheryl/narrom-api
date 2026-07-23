package com.naroom.api.global.error.response;

public record ValidationViolation(String field, String code, String message) {
}
