package com.naroom.api.lifetime.domain.entity;

import com.naroom.api.record.domain.entity.Entry;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "period_reflection_entries")
public class PeriodReflectionEntry {

	@EmbeddedId
	private PeriodReflectionEntryId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("periodReflectionId")
	@JoinColumn(name = "period_reflection_id", nullable = false, updatable = false)
	private PeriodReflection periodReflection;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("entryId")
	@JoinColumn(name = "entry_id", nullable = false, updatable = false)
	private Entry entry;

	// EMOTION/GRATITUDE/EFFORT/RECOVERY 등 근거 역할; 아직 고정된 코드 집합이 확정되지 않아 자유 문자열로 둔다.
	@Column(name = "evidence_role", length = 40)
	private String evidenceRole;

	@CreationTimestamp
	@Column(name = "linked_at", nullable = false, updatable = false)
	private Instant linkedAt;

	protected PeriodReflectionEntry() {
	}

	private PeriodReflectionEntry(PeriodReflection periodReflection, Entry entry, String evidenceRole) {
		this.periodReflection = periodReflection;
		this.entry = entry;
		this.evidenceRole = evidenceRole;
		this.id = new PeriodReflectionEntryId(periodReflection.getId(), entry.getId());
	}

	public static PeriodReflectionEntry link(PeriodReflection periodReflection, Entry entry, String evidenceRole) {
		return new PeriodReflectionEntry(periodReflection, entry, evidenceRole);
	}

	public PeriodReflectionEntryId getId() {
		return id;
	}

	public PeriodReflection getPeriodReflection() {
		return periodReflection;
	}

	public Entry getEntry() {
		return entry;
	}

	public String getEvidenceRole() {
		return evidenceRole;
	}

	public Instant getLinkedAt() {
		return linkedAt;
	}

}
