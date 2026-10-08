package za.co.bonalabs.bonahr.dto.employee;

import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.entity.EmploymentType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID organisationId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String jobTitle,
        String department,
        EmploymentType employmentType,
        LocalDate startDate,
        String phone,
        EmployeeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}