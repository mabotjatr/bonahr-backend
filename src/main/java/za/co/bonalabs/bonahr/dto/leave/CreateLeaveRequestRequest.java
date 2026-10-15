package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import za.co.bonalabs.bonahr.entity.LeaveDayPortion;

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

        LeaveDayPortion dayPortion,

        @Size(max = 1000)
        String reason) {

    public CreateLeaveRequestRequest(
            UUID employeeId,
            UUID leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {
        this(
                employeeId,
                leaveTypeId,
                startDate,
                endDate,
                LeaveDayPortion.FULL_DAY,
                reason);
    }
}