package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

	List<Tag> findByScopeAndActiveTrue(TagScope scope);

	List<Tag> findByOwnerMember_IdAndActiveTrue(UUID ownerMemberId);

	Optional<Tag> findByScopeAndCategoryAndNormalizedName(TagScope scope, TagCategory category, String normalizedName);

	Optional<Tag> findByOwnerMember_IdAndCategoryAndNormalizedName(
			UUID ownerMemberId, TagCategory category, String normalizedName);

}
