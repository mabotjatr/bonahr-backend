package za.co.bonalabs.bonahr.dto.employee;

import jakarta.validation.constraints.NotNull;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;

public record UpdateEmployeeStatusRequest(

        @NotNull
        EmployeeStatus status
) {
}