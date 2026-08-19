package com.naroom.api.admin.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// append-only. 애플리케이션에서 UPDATE/DELETE를 수행하지 않는다. 비밀키·토큰·쿠키·기록 원문·AI 입출력
// 원문은 절대 저장하지 않는다 - beforeData/afterData에도 비식별 메타데이터만 넣는다.
@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	// 인증 실패처럼 신원이 확정되지 않은 시도는 NULL일 수 있다.
	@Column(name = "actor_admin_id")
	private UUID actorAdminId;

	@Column(name = "action", nullable = false, length = 100)
	private String action;

	@Column(name = "resource_type", nullable = false, length = 100)
	private String resourceType;

	@Column(name = "resource_id", length = 255)
	private String resourceId;

	@Column(name = "before_data", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String beforeData;

	@Column(name = "after_data", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String afterData;

	@Column(name = "change_reason")
	private String changeReason;

	@Column(name = "trace_id", length = 100)
	private String traceId;

	@Column(name = "request_method", length = 10)
	private String requestMethod;

	@Column(name = "request_path", length = 500)
	private String requestPath;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, length = 10)
	private AdminAuditOutcome outcome;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AdminAuditLog() {
	}

	private AdminAuditLog(
			UUID actorAdminId,
			String action,
			String resourceType,
			String resourceId,
			String beforeData,
			String afterData,
			String changeReason,
			String traceId,
			String requestMethod,
			String requestPath,
			AdminAuditOutcome outcome) {
		this.actorAdminId = actorAdminId;
		this.action = action;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.beforeData = beforeData;
		this.afterData = afterData;
		this.changeReason = changeReason;
		this.traceId = traceId;
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.outcome = outcome;
	}

	public static AdminAuditLog record(
			UUID actorAdminId,
			String action,
			String resourceType,
			String resourceId,
			String beforeData,
			String afterData,
			String changeReason,
			String traceId,
			String requestMethod,
			String requestPath,
			AdminAuditOutcome outcome) {
		return new AdminAuditLog(
				actorAdminId, action, resourceType, resourceId, beforeData, afterData,
				changeReason, traceId, requestMethod, requestPath, outcome);
	}

	public UUID getId() {
		return id;
	}

	public UUID getActorAdminId() {
		return actorAdminId;
	}

	public String getAction() {
		return action;
	}

	public String getResourceType() {
		return resourceType;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getBeforeData() {
		return beforeData;
	}

	public String getAfterData() {
		return afterData;
	}

	public String getChangeReason() {
		return changeReason;
	}

	public String getTraceId() {
		return traceId;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public AdminAuditOutcome getOutcome() {
		return outcome;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
