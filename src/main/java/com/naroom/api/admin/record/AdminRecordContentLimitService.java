package com.naroom.api.admin.record;

import com.naroom.api.admin.record.dto.AdminRecordContentLimitResponse;
import com.naroom.api.admin.record.dto.AdminRecordContentLimitUpdateRequest;
import com.naroom.api.record.domain.entity.RecordContentLimit;
import com.naroom.api.record.domain.repository.RecordContentLimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// V31 마이그레이션이 심어둔 단일 행(RecordContentLimit.SINGLETON_ID)만 다룬다 - 새로 만들거나 지우지 않는다.
@Service
public class AdminRecordContentLimitService {

	private final RecordContentLimitRepository recordContentLimitRepository;

	public AdminRecordContentLimitService(RecordContentLimitRepository recordContentLimitRepository) {
		this.recordContentLimitRepository = recordContentLimitRepository;
	}

	@Transactional(readOnly = true)
	public AdminRecordContentLimitResponse get() {
		return AdminRecordContentLimitResponse.from(findSingleton());
	}

	@Transactional
	public AdminRecordContentLimitResponse update(AdminRecordContentLimitUpdateRequest request, UUID actingAdminId) {
		RecordContentLimit limit = findSingleton();
		limit.update(request.bodyMaxLength(), actingAdminId);
		return AdminRecordContentLimitResponse.from(limit);
	}

	private RecordContentLimit findSingleton() {
		return recordContentLimitRepository.findById(RecordContentLimit.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("record_content_limits singleton row missing"));
	}

}
