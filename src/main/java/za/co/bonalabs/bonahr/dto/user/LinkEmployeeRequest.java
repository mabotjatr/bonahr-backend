package za.co.bonalabs.bonahr.dto.user;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkEmployeeRequest(
        @NotNull UUID employeeId
) {
}