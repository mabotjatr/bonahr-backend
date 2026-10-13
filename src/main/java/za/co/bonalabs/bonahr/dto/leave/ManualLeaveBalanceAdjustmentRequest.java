package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ManualLeaveBalanceAdjustmentRequest(

        @NotNull
        BigDecimal amount,

        @Size(max = 1000)
        String comment
) {
}