package com.naroom.api.account.dto;

import java.time.Instant;

public record AccountWithdrawalResponse(Instant scheduledDeletionAt) {
}
