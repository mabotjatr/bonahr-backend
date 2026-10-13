package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLeavePolicyRequest(

        @NotNull
        UUID leaveTypeId,

        @NotNull
        LeaveAccrualMethod accrualMethod,

        @NotNull
        @Positive
        BigDecimal accrualRate,

        boolean allowNegative,

        @NotNull
        BigDecimal negativeLimitDays) {
}