package com.naroom.api.content;

import com.naroom.api.auth.security.MemberAuthentication;
import com.naroom.api.content.dto.QuoteResponse;
import com.naroom.api.content.dto.QuoteTopicResponse;
import com.naroom.api.content.dto.SavedQuoteResponse;
import com.naroom.api.global.response.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
public class ContentController {

	private final QuoteService quoteService;

	public ContentController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@GetMapping("/quotes/today")
	public ApiResponse<QuoteResponse> getTodayQuote() {
		return ApiResponse.of(quoteService.getTodayQuote(currentMemberId()));
	}

	@GetMapping("/quotes/{quoteId}")
	public ApiResponse<QuoteResponse> getQuote(@PathVariable UUID quoteId) {
		return ApiResponse.of(quoteService.getQuote(quoteId, currentMemberId()));
	}

	@GetMapping("/topics")
	public ApiResponse<List<QuoteTopicResponse>> getTopics() {
		return ApiResponse.of(quoteService.getActiveTopics());
	}

	@GetMapping("/topics/{topicId}/quotes")
	public ApiResponse<List<QuoteResponse>> getQuotesByTopic(@PathVariable UUID topicId) {
		return ApiResponse.of(quoteService.getQuotesByTopic(topicId, currentMemberId()));
	}

	@PostMapping("/quotes/{quoteId}/save")
	public void saveQuote(@PathVariable UUID quoteId) {
		quoteService.saveQuote(currentMemberId(), quoteId);
	}

	@DeleteMapping("/quotes/{quoteId}/save")
	public void unsaveQuote(@PathVariable UUID quoteId) {
		quoteService.unsaveQuote(currentMemberId(), quoteId);
	}

	@GetMapping("/quotes/saved")
	public ApiResponse<List<SavedQuoteResponse>> getSavedQuotes() {
		return ApiResponse.of(quoteService.getSavedQuotes(currentMemberId()));
	}

	// JwtAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접 꺼낸다
	// (AccountController와 동일한 이유).
	private UUID currentMemberId() {
		MemberAuthentication authentication =
				(MemberAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return authentication.getMemberId();
	}

}
