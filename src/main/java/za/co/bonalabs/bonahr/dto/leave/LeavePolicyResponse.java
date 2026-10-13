package za.co.bonalabs.bonahr.dto.leave;

import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record LeavePolicyResponse(
        UUID id,
        UUID leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LeaveAccrualMethod accrualMethod,
        BigDecimal accrualRate,
        boolean allowNegative,
        BigDecimal negativeLimitDays,
        boolean active
) {
}