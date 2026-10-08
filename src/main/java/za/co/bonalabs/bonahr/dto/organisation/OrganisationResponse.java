package za.co.bonalabs.bonahr.dto.organisation;

import za.co.bonalabs.bonahr.entity.OrganisationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganisationResponse(
        UUID id,
        String name,
        String legalName,
        String registrationNumber,
        String taxNumber,
        String email,
        String phone,
        String website,
        OrganisationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}