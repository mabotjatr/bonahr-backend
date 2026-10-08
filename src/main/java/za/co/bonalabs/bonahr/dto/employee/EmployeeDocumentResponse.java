package za.co.bonalabs.bonahr.dto.employee;

import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmployeeDocumentResponse(
        UUID id,
        UUID employeeId,
        EmployeeDocumentType documentType,
        String fileName,
        String storageKey,
        String mimeType,
        Long fileSize,
        UUID uploadedByUserId,
        OffsetDateTime createdAt
) {
}