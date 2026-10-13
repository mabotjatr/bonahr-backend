package za.co.bonalabs.bonahr.dto.leave;

import za.co.bonalabs.bonahr.entity.LeaveBalanceTransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveBalanceTransactionResponse(
        UUID id,
        UUID leaveAccountId,
        UUID leaveRequestId,
        LeaveBalanceTransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        BigDecimal reservedAfter,
        UUID actorUserId,
        String comment,
        OffsetDateTime createdAt
) {
}