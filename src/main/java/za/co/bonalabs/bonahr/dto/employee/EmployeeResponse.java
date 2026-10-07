package za.co.bonalabs.bonahr.dto.employee;

import za.co.bonalabs.bonahr.entity.EmployeeStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID organisationId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        EmployeeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}