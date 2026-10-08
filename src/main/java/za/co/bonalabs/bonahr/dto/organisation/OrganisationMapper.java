package za.co.bonalabs.bonahr.dto.organisation;

import za.co.bonalabs.bonahr.entity.Organisation;

public final class OrganisationMapper {

    private OrganisationMapper() {
    }

    public static OrganisationResponse toResponse(Organisation organisation) {
        return new OrganisationResponse(
                organisation.getId(),
                organisation.getName(),
                organisation.getLegalName(),
                organisation.getRegistrationNumber(),
                organisation.getTaxNumber(),
                organisation.getEmail(),
                organisation.getPhone(),
                organisation.getWebsite(),
                organisation.getStatus(),
                organisation.getCreatedAt(),
                organisation.getUpdatedAt()
        );
    }
}