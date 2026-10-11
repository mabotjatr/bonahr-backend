package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateLeaveRequestRequest(

        @NotNull
        UUID employeeId,

        @NotNull
        UUID leaveTypeId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @Size(max = 1000)
        String reason
) {
}