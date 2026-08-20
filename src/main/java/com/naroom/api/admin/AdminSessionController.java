package com.naroom.api.admin;

import com.naroom.api.admin.auth.AdminAuthentication;
import com.naroom.api.admin.dto.AdminSessionResponse;
import com.naroom.api.global.response.ApiResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminSessionController {

	private final AdminSessionQueryService adminSessionQueryService;

	public AdminSessionController(AdminSessionQueryService adminSessionQueryService) {
		this.adminSessionQueryService = adminSessionQueryService;
	}

	@GetMapping("/session")
	public ApiResponse<AdminSessionResponse> session() {
		// AdminSessionAuthenticationFilter가 SecurityContextHolder에 직접 채워 넣는 방식이라 여기서도 직접
		// 꺼낸다(Authentication 파라미터 자동 바인딩에 의존하지 않는다) - AuthController.session()과 동일한 이유.
		AdminAuthentication authentication = (AdminAuthentication) SecurityContextHolder.getContext().getAuthentication();
		return ApiResponse.of(adminSessionQueryService.check(authentication.getAdminUserId(), authentication.getAdminSessionId()));
	}

}
