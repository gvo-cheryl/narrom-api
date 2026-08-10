package com.naroom.api.account.domain.repository;

import com.naroom.api.account.domain.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
}
