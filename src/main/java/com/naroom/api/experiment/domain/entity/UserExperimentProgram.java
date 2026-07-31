package com.naroom.api.experiment.domain.entity;

import com.naroom.api.account.domain.entity.Member;
import com.naroom.api.record.domain.entity.Entry;
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
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

// 사용자가 저장·시작한 실제 코스. program이 TEMPLATE 시작이면 채워지고, USER_COMPOSED/RANDOM이면
// null일 수 있다(§12.1). 활성 상태(IN_PROGRESS/PAUSED/AWAITING_REVIEW)는 회원당 1개만 허용하는
// 제약이 DB 부분 유니크 인덱스(uq_user_experiment_one_active)에 있고, version은 동시 요청으로
// 같은 미션이 두 번 소비되지 않도록 하는 낙관적 잠금이다(§13 미션 기록 트랜잭션).
@Entity
@Table(name = "user_experiment_programs")
public class UserExperimentProgram {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "program_id", updatable = false)
	private ExperimentProgram program;

	@Column(name = "program_version")
	private Integer programVersion;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "configuration_source", nullable = false, updatable = false)
	private ExperimentSourceType configurationSource;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false)
	private UserExperimentProgramStatus status;

	@Column(name = "title_snapshot", nullable = false, length = 120)
	private String titleSnapshot;

	@Column(name = "description_snapshot", nullable = false)
	private String descriptionSnapshot;

	@Column(name = "duration_days", nullable = false, updatable = false)
	private short durationDays;

	@Column(name = "current_day", nullable = false)
	private short currentDay;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "target_end_date")
	private LocalDate targetEndDate;

	@Column(name = "paused_at")
	private Instant pausedAt;

	@Column(name = "review_ready_at")
	private Instant reviewReadyAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "ended_early_at")
	private Instant endedEarlyAt;

	@Column(name = "last_activity_date")
	private LocalDate lastActivityDate;

	@Column(name = "review_data", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private String reviewData;

	@Column(name = "user_summary")
	private String userSummary;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_entry_id")
	private Entry reviewEntry;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected UserExperimentProgram() {
	}

	private UserExperimentProgram(
			Member member, ExperimentProgram program, Integer programVersion,
			ExperimentSourceType configurationSource, String titleSnapshot, String descriptionSnapshot,
			short durationDays) {
		this.member = member;
		this.program = program;
		this.programVersion = programVersion;
		this.configurationSource = configurationSource;
		this.status = UserExperimentProgramStatus.READY;
		this.titleSnapshot = titleSnapshot;
		this.descriptionSnapshot = descriptionSnapshot;
		this.durationDays = durationDays;
		this.currentDay = 1;
	}

	// §13 코스 시작 트랜잭션의 3단계(user_experiment_programs 생성)다. TEMPLATE/RANDOM/USER_COMPOSED
	// 모두 이 팩토리 하나를 쓴다 - 차이는 program/programVersion이 있는지뿐이다. 곧바로 IN_PROGRESS로
	// 만들지 않고 READY로 만드는 이유는 "저장해두고 나중에 시작하기"(§5.2)도 같은 생성 경로를 타야
	// 하기 때문이다 - 실제 시작은 activate()가 별도로 한다.
	public static UserExperimentProgram ready(
			Member member, ExperimentProgram program, Integer programVersion,
			ExperimentSourceType configurationSource, String titleSnapshot, String descriptionSnapshot,
			short durationDays) {
		return new UserExperimentProgram(
				member, program, programVersion, configurationSource, titleSnapshot, descriptionSnapshot, durationDays);
	}

	// READY -> IN_PROGRESS. Day 1 슬롯을 CURRENT로 올리는 것은 서비스 계층이 UserProgramMission에
	// 대해 별도로 한다(§13 5단계) - 엔티티는 자기 자신의 상태·시각만 책임진다. "오늘"의 기준은
	// PeriodCalculator와 동일하게 회원의 timezone이다 - UTC로 고정하면 자정 근처에 시작한 사용자의
	// 코스 기간이 하루 밀리거나 당겨질 수 있다.
	public void activate(Instant now) {
		this.status = UserExperimentProgramStatus.IN_PROGRESS;
		this.startedAt = now;
		LocalDate today = now.atZone(ZoneId.of(member.getTimezone())).toLocalDate();
		this.targetEndDate = today.plusDays(durationDays - 1L);
		this.lastActivityDate = today;
	}

	// §5.5 활성 코스 충돌 - "여기까지 기록하고 새 코스로 바꾸기"에서 쓴다. 지금까지의 기록은 그대로
	// 두고(§11.2 원칙과 동일하게 삭제하지 않음) 상태만 종료로 바꾼다 - 돌아보기(review)는 선택 사항이라
	// 강제하지 않는다.
	public void endEarly(Instant now) {
		this.status = UserExperimentProgramStatus.ENDED_EARLY;
		this.endedEarlyAt = now;
	}

	public UUID getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public ExperimentProgram getProgram() {
		return program;
	}

	public Integer getProgramVersion() {
		return programVersion;
	}

	public ExperimentSourceType getConfigurationSource() {
		return configurationSource;
	}

	public UserExperimentProgramStatus getStatus() {
		return status;
	}

	public String getTitleSnapshot() {
		return titleSnapshot;
	}

	public String getDescriptionSnapshot() {
		return descriptionSnapshot;
	}

	public short getDurationDays() {
		return durationDays;
	}

	public short getCurrentDay() {
		return currentDay;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public LocalDate getTargetEndDate() {
		return targetEndDate;
	}

	public Instant getPausedAt() {
		return pausedAt;
	}

	public Instant getReviewReadyAt() {
		return reviewReadyAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public Instant getEndedEarlyAt() {
		return endedEarlyAt;
	}

	public LocalDate getLastActivityDate() {
		return lastActivityDate;
	}

	public String getReviewData() {
		return reviewData;
	}

	public String getUserSummary() {
		return userSummary;
	}

	public Entry getReviewEntry() {
		return reviewEntry;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}

}
