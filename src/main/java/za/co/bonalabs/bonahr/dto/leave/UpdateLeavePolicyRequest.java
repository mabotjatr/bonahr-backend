package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;

import java.math.BigDecimal;

public record UpdateLeavePolicyRequest(

        @NotNull
        LeaveAccrualMethod accrualMethod,

        @NotNull
        @Positive
        BigDecimal accrualRate,

        boolean allowNegative,

        @NotNull
        BigDecimal negativeLimitDays
) {
}