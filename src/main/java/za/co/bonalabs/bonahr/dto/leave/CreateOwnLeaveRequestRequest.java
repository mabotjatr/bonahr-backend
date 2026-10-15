package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.co.bonalabs.bonahr.entity.LeaveDayPortion;

import java.time.LocalDate;
import java.util.UUID;

public record CreateOwnLeaveRequestRequest(
        @NotNull UUID leaveTypeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LeaveDayPortion dayPortion,
        @Size(max = 1000) String reason
) {
}