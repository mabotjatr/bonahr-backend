package za.co.bonalabs.bonahr.dto.leave;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveTypeResponse(
        UUID id,
        UUID organisationId,
        String code,
        String name,
        boolean paid,
        boolean requiresApproval,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}