package com.naroom.api.admin.audit;

import com.naroom.api.admin.audit.dto.AdminAuditLogResponse;
import com.naroom.api.admin.domain.entity.AdminAuditLog;
import com.naroom.api.admin.domain.entity.AdminAuditOutcome;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminAuditLogRepository;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import com.naroom.api.global.response.CursorPageResponse;
import com.naroom.api.global.response.PageInfo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

// append-only 기록만 한다 - update/delete 메서드를 두지 않는다. 로그인 실패처럼 요청을 처리하는 트랜잭션이
// 실패해도 감사 기록 자체는 남아야 하므로 REQUIRES_NEW로 별도 커밋한다.
@Service
public class AdminAuditLogService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final AdminAuditLogRepository adminAuditLogRepository;
	private final AdminUserRepository adminUserRepository;

	public AdminAuditLogService(AdminAuditLogRepository adminAuditLogRepository, AdminUserRepository adminUserRepository) {
		this.adminAuditLogRepository = adminAuditLogRepository;
		this.adminUserRepository = adminUserRepository;
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

	@Transactional(readOnly = true)
	public CursorPageResponse<AdminAuditLogResponse> list(
			UUID actorAdminId,
			String action,
			String resourceType,
			AdminAuditOutcome outcome,
			Instant from,
			Instant to,
			String cursor,
			Integer size) {
		int pageSize = Math.min(size != null ? size : DEFAULT_SIZE, MAX_SIZE);
		Specification<AdminAuditLog> spec = buildSpecification(actorAdminId, action, resourceType, outcome, from, to, cursor);
		Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

		List<AdminAuditLog> rows = adminAuditLogRepository.findAll(spec, PageRequest.of(0, pageSize + 1, sort)).getContent();
		boolean hasNext = rows.size() > pageSize;
		List<AdminAuditLog> page = hasNext ? rows.subList(0, pageSize) : rows;

		Map<UUID, String> actorNames = resolveActorNames(page);
		List<AdminAuditLogResponse> data = page.stream()
				.map(log -> AdminAuditLogResponse.of(
						log, log.getActorAdminId() != null ? actorNames.get(log.getActorAdminId()) : null))
				.toList();

		String nextCursor = hasNext
				? new AdminAuditLogCursor(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId()).encode()
				: null;
		return CursorPageResponse.of(data, new PageInfo(nextCursor, hasNext));
	}

	private Specification<AdminAuditLog> buildSpecification(
			UUID actorAdminId, String action, String resourceType, AdminAuditOutcome outcome,
			Instant from, Instant to, String cursor) {
		Specification<AdminAuditLog> spec = Specification.unrestricted();
		if (actorAdminId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("actorAdminId"), actorAdminId));
		}
		if (action != null && !action.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
		}
		if (resourceType != null && !resourceType.isBlank()) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("resourceType"), resourceType));
		}
		if (outcome != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("outcome"), outcome));
		}
		if (from != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
		}
		if (to != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), to));
		}
		if (cursor != null && !cursor.isBlank()) {
			AdminAuditLogCursor decoded = AdminAuditLogCursor.decode(cursor);
			spec = spec.and((root, query, cb) -> cb.or(
					cb.lessThan(root.<Instant>get("createdAt"), decoded.createdAt()),
					cb.and(
							cb.equal(root.get("createdAt"), decoded.createdAt()),
							cb.lessThan(root.<UUID>get("id"), decoded.id()))));
		}
		return spec;
	}

	private Map<UUID, String> resolveActorNames(List<AdminAuditLog> logs) {
		List<UUID> actorIds = logs.stream().map(AdminAuditLog::getActorAdminId).filter(Objects::nonNull).distinct().toList();
		if (actorIds.isEmpty()) {
			return Map.of();
		}
		return adminUserRepository.findAllById(actorIds).stream()
				.collect(Collectors.toMap(AdminUser::getId, AdminUser::getDisplayName));
	}

}
