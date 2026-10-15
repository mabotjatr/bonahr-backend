package za.co.bonalabs.bonahr.dto.leave;

import za.co.bonalabs.bonahr.entity.LeaveRequestStatus;
import za.co.bonalabs.bonahr.entity.LeaveDayPortion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveRequestResponse(
        UUID id,
        UUID organisationId,
        UUID employeeId,
        UUID leaveTypeId,
        LocalDate startDate,
        LocalDate endDate,
        LeaveDayPortion dayPortion,
        LeaveRequestStatus status,
        String reason,
        String decisionComment,
        UUID requestedByUserId,
        UUID decidedByUserId,
        OffsetDateTime requestedAt,
        OffsetDateTime decidedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}