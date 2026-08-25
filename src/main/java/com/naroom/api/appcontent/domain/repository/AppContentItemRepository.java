package com.naroom.api.appcontent.domain.repository;

import com.naroom.api.appcontent.domain.entity.AppContentItem;
import com.naroom.api.appcontent.domain.entity.AppContentItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppContentItemRepository extends JpaRepository<AppContentItem, UUID>, JpaSpecificationExecutor<AppContentItem> {

	boolean existsByContentKeyAndLocale(String contentKey, String locale);

	Optional<AppContentItem> findByContentKeyAndLocaleAndStatus(
			String contentKey, String locale, AppContentItemStatus status);

}
