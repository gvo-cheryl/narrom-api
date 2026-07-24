package com.naroom.api.content.domain.repository;

import com.naroom.api.content.domain.entity.MemberSavedQuote;
import com.naroom.api.content.domain.entity.MemberSavedQuoteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberSavedQuoteRepository extends JpaRepository<MemberSavedQuote, MemberSavedQuoteId> {

	List<MemberSavedQuote> findByMember_IdOrderBySavedAtDesc(UUID memberId);

}
