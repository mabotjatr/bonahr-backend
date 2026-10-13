package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEmployeeLeaveAccountRequest(

        @NotNull
        UUID employeeId,

        @NotNull
        UUID leaveTypeId) {
}