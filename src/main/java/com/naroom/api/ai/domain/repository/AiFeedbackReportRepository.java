package com.naroom.api.ai.domain.repository;

import com.naroom.api.ai.domain.entity.AiFeedbackReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiFeedbackReportRepository extends JpaRepository<AiFeedbackReport, UUID> {
}
