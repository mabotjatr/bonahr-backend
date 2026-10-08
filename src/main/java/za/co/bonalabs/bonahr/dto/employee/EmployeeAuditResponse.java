package za.co.bonalabs.bonahr.dto.employee;

import za.co.bonalabs.bonahr.entity.EmployeeAuditAction;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EmployeeAuditResponse(
        UUID id,
        UUID employeeId,
        EmployeeAuditAction action,
        UUID actorUserId,
        Map<String, Object> changes,
        OffsetDateTime createdAt
) {
}