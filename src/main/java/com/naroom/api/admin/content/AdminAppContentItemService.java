package com.naroom.api.admin.content;

import com.naroom.api.admin.content.dto.AdminAppContentItemCreateRequest;
import com.naroom.api.admin.content.dto.AdminAppContentItemResponse;
import com.naroom.api.admin.content.dto.AdminAppContentItemUpdateRequest;
import com.naroom.api.appcontent.domain.entity.AppContentItem;
import com.naroom.api.appcontent.domain.entity.AppContentItemStatus;
import com.naroom.api.appcontent.domain.error.AppContentErrorCode;
import com.naroom.api.appcontent.domain.repository.AppContentItemRepository;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminAppContentItemService {

	private static final String DEFAULT_LOCALE = "ko-KR";

	private final AppContentItemRepository appContentItemRepository;

	public AdminAppContentItemService(AppContentItemRepository appContentItemRepository) {
		this.appContentItemRepository = appContentItemRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminAppContentItemResponse> list() {
		return appContentItemRepository.findAllByOrderByContentKeyAscLocaleAscVersionNoDesc().stream()
				.map(AdminAppContentItemResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AdminAppContentItemResponse get(UUID id) {
		return AdminAppContentItemResponse.from(findOrThrow(id));
	}

	@Transactional
	public AdminAppContentItemResponse create(AdminAppContentItemCreateRequest request, UUID actingAdminId) {
		String locale = resolveLocale(request.locale());
		if (appContentItemRepository.existsByContentKeyAndLocale(request.contentKey(), locale)) {
			throw new BusinessException(AppContentErrorCode.ITEM_KEY_ALREADY_EXISTS);
		}
		AppContentItem item = AppContentItem.create(
				request.contentKey(), request.surface(), locale, request.valueType(), request.valueText(),
				request.valueJson(), request.schemaVersion(), 1, request.activeFrom(), request.activeUntil(),
				request.fallbackRequired(), actingAdminId, null);
		return AdminAppContentItemResponse.from(appContentItemRepository.save(item));
	}

	@Transactional
	public AdminAppContentItemResponse update(UUID id, AdminAppContentItemUpdateRequest request) {
		AppContentItem item = findOrThrow(id);
		requireStatus(item, AppContentItemStatus.DRAFT);
		item.updateDraft(
				request.surface(), request.valueType(), request.valueText(), request.valueJson(),
				request.schemaVersion(), request.activeFrom(), request.activeUntil(), request.fallbackRequired());
		return AdminAppContentItemResponse.from(item);
	}

	// §19.4 편집 API 버전 규칙: 발행본은 직접 수정하지 않고, 발행본 내용을 시작점으로 하는 새 DRAFT를 만든다.
	@Transactional
	public AdminAppContentItemResponse createRevision(UUID publishedId, UUID actingAdminId) {
		AppContentItem published = findOrThrow(publishedId);
		requireStatus(published, AppContentItemStatus.PUBLISHED);
		AppContentItem draft = AppContentItem.create(
				published.getContentKey(), published.getSurface(), published.getLocale(), published.getValueType(),
				published.getValueText(), published.getValueJson(), published.getSchemaVersion(),
				published.getVersionNo() + 1, published.getActiveFrom(), published.getActiveUntil(),
				published.isFallbackRequired(), actingAdminId, published.getId());
		return AdminAppContentItemResponse.from(appContentItemRepository.save(draft));
	}

	// 같은 content_key+locale로 이미 PUBLISHED된 버전이 있으면 먼저 ARCHIVED로 내린다(app_content_items는
	// content_key+locale당 PUBLISHED 1개만 허용).
	@Transactional
	public AdminAppContentItemResponse publish(UUID draftId) {
		AppContentItem draft = findOrThrow(draftId);
		requireStatus(draft, AppContentItemStatus.DRAFT);
		appContentItemRepository
				.findByContentKeyAndLocaleAndStatus(draft.getContentKey(), draft.getLocale(), AppContentItemStatus.PUBLISHED)
				.ifPresent(AppContentItem::archive);
		draft.publish();
		return AdminAppContentItemResponse.from(draft);
	}

	@Transactional
	public AdminAppContentItemResponse archive(UUID id) {
		AppContentItem item = findOrThrow(id);
		requireStatus(item, AppContentItemStatus.PUBLISHED);
		item.archive();
		return AdminAppContentItemResponse.from(item);
	}

	private String resolveLocale(String locale) {
		return locale == null || locale.isBlank() ? DEFAULT_LOCALE : locale;
	}

	private void requireStatus(AppContentItem item, AppContentItemStatus expected) {
		if (item.getStatus() != expected) {
			throw new BusinessException(
					expected == AppContentItemStatus.DRAFT
							? AppContentErrorCode.ITEM_NOT_DRAFT
							: AppContentErrorCode.ITEM_NOT_PUBLISHED);
		}
	}

	private AppContentItem findOrThrow(UUID id) {
		return appContentItemRepository.findById(id)
				.orElseThrow(() -> new BusinessException(AppContentErrorCode.ITEM_NOT_FOUND));
	}

}
