package com.naroom.api.experiment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// 코스 시작 시 복사된 사용자별 일차 슬롯. mission/originalMission이 nullable인 이유는 원본 미션이
// 삭제돼도(ON DELETE SET NULL) 스냅샷 문구(titleSnapshot 등)는 그대로 남아 과거 기록이 깨지지
// 않게 하기 위함이다(§12.2).
@Entity
@Table(name = "user_program_missions")
public class UserProgramMission {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_experiment_program_id", nullable = false, updatable = false)
	private UserExperimentProgram userExperimentProgram;

	@Column(name = "day_number", nullable = false, updatable = false)
	private short dayNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private ExperimentMission mission;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_mission_id")
	private ExperimentMission originalMission;

	@Column(name = "mission_version", nullable = false)
	private int missionVersion;

	@Column(name = "title_snapshot", nullable = false, length = 120)
	private String titleSnapshot;

	@Column(name = "description_snapshot", nullable = false)
	private String descriptionSnapshot;

	@Column(name = "instruction_snapshot", nullable = false)
	private String instructionSnapshot;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "mission_type", nullable = false)
	private ExperimentMissionType missionType;

	@Column(name = "response_type", nullable = false, length = 40)
	private String responseType;

	@Column(name = "estimated_minutes", nullable = false)
	private short estimatedMinutes;

	@Column(name = "reflection_questions_snapshot", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String reflectionQuestionsSnapshot;

	@Column(name = "response_schema_snapshot", columnDefinition = "jsonb", nullable = false)
	@JdbcTypeCode(SqlTypes.JSON)
	private String responseSchemaSnapshot;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "slot_status", nullable = false)
	private UserProgramMissionSlotStatus slotStatus;

	@Column(name = "replacement_count", nullable = false)
	private int replacementCount;

	@Column(name = "first_available_at")
	private Instant firstAvailableAt;

	@Column(name = "recorded_at")
	private Instant recordedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserProgramMission() {
	}

	private UserProgramMission(
			UserExperimentProgram userExperimentProgram, short dayNumber, ExperimentMission mission,
			int missionVersion, String titleSnapshot, String descriptionSnapshot, String instructionSnapshot,
			ExperimentMissionType missionType, String responseType, short estimatedMinutes,
			String reflectionQuestionsSnapshot, String responseSchemaSnapshot) {
		this.userExperimentProgram = userExperimentProgram;
		this.dayNumber = dayNumber;
		this.mission = mission;
		this.originalMission = mission;
		this.missionVersion = missionVersion;
		this.titleSnapshot = titleSnapshot;
		this.descriptionSnapshot = descriptionSnapshot;
		this.instructionSnapshot = instructionSnapshot;
		this.missionType = missionType;
		this.responseType = responseType;
		this.estimatedMinutes = estimatedMinutes;
		this.reflectionQuestionsSnapshot = reflectionQuestionsSnapshot;
		this.responseSchemaSnapshot = responseSchemaSnapshot;
		this.slotStatus = UserProgramMissionSlotStatus.PENDING;
		this.replacementCount = 0;
	}

	// §12.2 스냅샷: 카탈로그 미션(TEMPLATE/RANDOM)의 문구를 그대로 복사한다 - 원본이 나중에 수정돼도
	// 이미 시작한 사용자의 슬롯은 바뀌지 않는다.
	public static UserProgramMission fromCatalog(
			UserExperimentProgram userExperimentProgram, short dayNumber, ExperimentMission mission) {
		return new UserProgramMission(
				userExperimentProgram, dayNumber, mission, mission.getContentVersion(),
				mission.getTitle(), mission.getDescription(), mission.getInstruction(),
				mission.getMissionType(), mission.getResponseType(), mission.getEstimatedMinutes(),
				mission.getReflectionQuestions(), mission.getResponseSchema());
	}

	// USER_COMPOSED: 카탈로그 미션이 아니라 사용자가 직접 적은 문구로 슬롯을 만든다 - mission/
	// originalMission은 연결할 카탈로그 행이 없어 null이다. Beta 1은 자유 응답(TEXT)만 지원하고
	// 되돌아볼 질문 목록·응답 스키마는 아직 받지 않는다(§4 P0-After: AI가 자유 생성한 미션은 제외).
	public static UserProgramMission userComposed(
			UserExperimentProgram userExperimentProgram, short dayNumber, String title, String instruction,
			ExperimentMissionType missionType, short estimatedMinutes) {
		UserProgramMission slot = new UserProgramMission(
				userExperimentProgram, dayNumber, null, 1,
				title, "", instruction, missionType, "TEXT", estimatedMinutes, "[]", "{}");
		slot.mission = null;
		slot.originalMission = null;
		return slot;
	}

	// §13 5단계: Day 1만 즉시 CURRENT로 올린다. 나머지 날짜는 PENDING으로 남아 이전 슬롯이
	// RECORDED가 될 때 차례로 CURRENT가 된다(§13 미션 기록 트랜잭션 6단계).
	public void promoteToCurrent(Instant now) {
		this.slotStatus = UserProgramMissionSlotStatus.CURRENT;
		this.firstAvailableAt = now;
	}

	// §13 미션 기록 트랜잭션 5단계: RESTED가 아닌 시도 상태를 기록하면 슬롯을 소비한다.
	public void recordAttempt(Instant now) {
		this.slotStatus = UserProgramMissionSlotStatus.RECORDED;
		this.recordedAt = now;
	}

	// §13 미션 교체 트랜잭션 4~5단계. originalMission은 최초 시작 시 배정된 미션을 계속 가리켜야
	// 하므로 여기서 건드리지 않는다 - "원래 무엇이었는지"는 교체를 여러 번 해도 바뀌지 않는다.
	public void replaceMission(ExperimentMission newMission) {
		this.mission = newMission;
		this.missionVersion = newMission.getContentVersion();
		this.titleSnapshot = newMission.getTitle();
		this.descriptionSnapshot = newMission.getDescription();
		this.instructionSnapshot = newMission.getInstruction();
		this.missionType = newMission.getMissionType();
		this.responseType = newMission.getResponseType();
		this.estimatedMinutes = newMission.getEstimatedMinutes();
		this.reflectionQuestionsSnapshot = newMission.getReflectionQuestions();
		this.responseSchemaSnapshot = newMission.getResponseSchema();
		this.replacementCount = this.replacementCount + 1;
	}

	public UUID getId() {
		return id;
	}

	public UserExperimentProgram getUserExperimentProgram() {
		return userExperimentProgram;
	}

	public short getDayNumber() {
		return dayNumber;
	}

	public ExperimentMission getMission() {
		return mission;
	}

	public ExperimentMission getOriginalMission() {
		return originalMission;
	}

	public int getMissionVersion() {
		return missionVersion;
	}

	public String getTitleSnapshot() {
		return titleSnapshot;
	}

	public String getDescriptionSnapshot() {
		return descriptionSnapshot;
	}

	public String getInstructionSnapshot() {
		return instructionSnapshot;
	}

	public ExperimentMissionType getMissionType() {
		return missionType;
	}

	public String getResponseType() {
		return responseType;
	}

	public short getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public String getReflectionQuestionsSnapshot() {
		return reflectionQuestionsSnapshot;
	}

	public String getResponseSchemaSnapshot() {
		return responseSchemaSnapshot;
	}

	public UserProgramMissionSlotStatus getSlotStatus() {
		return slotStatus;
	}

	public int getReplacementCount() {
		return replacementCount;
	}

	public Instant getFirstAvailableAt() {
		return firstAvailableAt;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
