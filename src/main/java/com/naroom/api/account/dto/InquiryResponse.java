package com.naroom.api.account.dto;

import com.naroom.api.account.domain.entity.Inquiry;

import java.time.Instant;
import java.util.UUID;

public record InquiryResponse(UUID id, Instant createdAt) {

	public static InquiryResponse from(Inquiry inquiry) {
		return new InquiryResponse(inquiry.getId(), inquiry.getCreatedAt());
	}

}
