package com.naroom.api.record.domain.repository;

import com.naroom.api.record.domain.entity.RecordContentLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecordContentLimitRepository extends JpaRepository<RecordContentLimit, UUID> {
}
