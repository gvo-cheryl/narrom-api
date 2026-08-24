package com.naroom.api.admin.content.dto;

import com.naroom.api.appcontent.domain.entity.AppContentItem;
import com.naroom.api.appcontent.domain.entity.AppContentItemStatus;
import com.naroom.api.appcontent.domain.entity.AppContentValueType;

import java.time.Instant;
import java.util.UUID;

public record AdminAppContentItemResponse(
		UUID id,
		String contentKey,
		String surface,
		String locale,
		AppContentValueType valueType,
		String valueText,
		String valueJson,
		String schemaVersion,
		int versionNo,
		AppContentItemStatus status,
		Instant activeFrom,
		Instant activeUntil,
		boolean fallbackRequired,
		UUID createdByAdminId,
		UUID supersedesItemId,
		Instant createdAt,
		Instant updatedAt) {

	public static AdminAppContentItemResponse from(AppContentItem item) {
		return new AdminAppContentItemResponse(
				item.getId(),
				item.getContentKey(),
				item.getSurface(),
				item.getLocale(),
				item.getValueType(),
				item.getValueText(),
				item.getValueJson(),
				item.getSchemaVersion(),
				item.getVersionNo(),
				item.getStatus(),
				item.getActiveFrom(),
				item.getActiveUntil(),
				item.isFallbackRequired(),
				item.getCreatedByAdminId(),
				item.getSupersedesItemId(),
				item.getCreatedAt(),
				item.getUpdatedAt());
	}

}
