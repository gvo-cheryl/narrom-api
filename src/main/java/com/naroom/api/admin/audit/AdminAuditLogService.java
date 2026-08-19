package com.naroom.api.admin.audit;

import com.naroom.api.admin.domain.entity.AdminAuditLog;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// append-only 기록만 한다 - update/delete 메서드를 두지 않는다. 로그인 실패처럼 요청을 처리하는 트랜잭션이
// 실패해도 감사 기록 자체는 남아야 하므로 REQUIRES_NEW로 별도 커밋한다.
@Service
public class AdminAuditLogService {

	private final AdminAuditLogRepository adminAuditLogRepository;

	public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository) {
		this.adminAuditLogRepository = adminAuditLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(
			UUID actorAdminId,
			String action,
			String resourceType,
			String resourceId,
			String changeReason,
			String traceId,
			String requestMethod,
			String requestPath,
			AdminAuditOutcome outcome) {
		adminAuditLogRepository.save(AdminAuditLog.record(
				actorAdminId, action, resourceType, resourceId, null, null,
				changeReason, traceId, requestMethod, requestPath, outcome));
	}

}
