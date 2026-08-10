package com.naroom.api.account;

import com.naroom.api.account.domain.entity.Inquiry;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.InquiryRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.InquiryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InquiryService {

	private final InquiryRepository inquiryRepository;
	private final MemberRepository memberRepository;

	public InquiryService(InquiryRepository inquiryRepository, MemberRepository memberRepository) {
		this.inquiryRepository = inquiryRepository;
		this.memberRepository = memberRepository;
	}

	@Transactional
	public InquiryResponse submit(UUID memberId, String content) {
		Member member = memberRepository.getReferenceById(memberId);
		Inquiry inquiry = inquiryRepository.save(Inquiry.create(member, content));
		return InquiryResponse.from(inquiry);
	}

}
