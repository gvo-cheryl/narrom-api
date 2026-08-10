package com.naroom.api.account;

import com.naroom.api.account.domain.entity.Inquiry;
import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.account.domain.repository.InquiryRepository;
import com.naroom.api.account.domain.repository.MemberRepository;
import com.naroom.api.account.dto.InquiryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
@DirtiesContext
class InquiryServiceTest {

	@Autowired
	private InquiryService inquiryService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Test
	void submit_validRequest_savesInquiryForMember() {
		Member member = memberRepository.save(Member.create("지연"));

		InquiryResponse response = inquiryService.submit(member.getId(), "로그인이 자꾸 풀려요");

		assertNotNull(response.id());
		Inquiry reloaded = inquiryRepository.findById(response.id()).orElseThrow();
		assertEquals(member.getId(), reloaded.getMember().getId());
		assertEquals("로그인이 자꾸 풀려요", reloaded.getContent());
	}

	@Test
	void submit_calledTwice_createsSeparateInquiries() {
		Member member = memberRepository.save(Member.create("지연"));

		InquiryResponse first = inquiryService.submit(member.getId(), "문의 1");
		InquiryResponse second = inquiryService.submit(member.getId(), "문의 2");

		assertEquals(2, inquiryRepository.count());
		assertNotNull(first.id());
		assertNotNull(second.id());
	}

}
