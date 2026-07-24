package com.naroom.api.record;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.record.domain.entity.Tag;
import com.naroom.api.record.domain.entity.TagCategory;
import com.naroom.api.record.domain.entity.TagScope;
import com.naroom.api.record.domain.error.RecordErrorCode;
import com.naroom.api.record.domain.repository.TagRepository;
import com.naroom.api.record.dto.TagResponse;
import com.naroom.api.record.dto.UserTagCreateRequest;
import com.naroom.api.global.error.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TagService {

	private final TagRepository tagRepository;
	private final MemberRepository memberRepository;

	public TagService(TagRepository tagRepository, MemberRepository memberRepository) {
		this.tagRepository = tagRepository;
		this.memberRepository = memberRepository;
	}

	public List<TagResponse> listSystemTags() {
		return tagRepository.findByScopeAndActiveTrue(TagScope.SYSTEM).stream()
				.map(TagResponse::from)
				.collect(Collectors.toList());
	}

	public List<TagResponse> listUserTags(UUID memberId) {
		return tagRepository.findByOwnerMember_IdAndActiveTrue(memberId).stream()
				.map(TagResponse::from)
				.collect(Collectors.toList());
	}

	// 같은 회원이 같은 분류·이름의 태그를 다시 만들어도 중복 생성하지 않고 기존 태그를 반환한다.
	@Transactional
	public TagResponse createUserTag(UUID memberId, UserTagCreateRequest request) {
		String normalizedName = normalize(request.name());
		return tagRepository
				.findByOwnerMember_IdAndCategoryAndNormalizedName(memberId, request.category(), normalizedName)
				.map(TagResponse::from)
				.orElseGet(() -> {
					Member owner = memberRepository.getReferenceById(memberId);
					Tag tag = Tag.createUserTag(owner, request.category(), request.name(), normalizedName);
					return TagResponse.from(tagRepository.save(tag));
				});
	}

	Tag getTagOrThrow(UUID tagId) {
		return tagRepository.findById(tagId)
				.orElseThrow(() -> new BusinessException(RecordErrorCode.TAG_NOT_FOUND));
	}

	private String normalize(String name) {
		return name.trim();
	}

}
