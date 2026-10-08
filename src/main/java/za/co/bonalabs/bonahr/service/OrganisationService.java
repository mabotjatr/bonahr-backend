package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.organisation.CreateOrganisationRequest;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.UUID;

@Service
@Transactional
public class OrganisationService {

    private final OrganisationRepository organisationRepository;

    public OrganisationService(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    public Organisation createOrganisation(CreateOrganisationRequest request) {

        Organisation organisation = new Organisation(request.name());

        organisation.setLegalName(request.legalName());
        organisation.setRegistrationNumber(request.registrationNumber());
        organisation.setTaxNumber(request.taxNumber());
        organisation.setEmail(request.email());
        organisation.setPhone(request.phone());
        organisation.setWebsite(request.website());

        return organisationRepository.save(organisation);
    }

    @Transactional(readOnly = true)
    public Organisation getOrganisation(UUID id, UUID authenticatedOrganisationId) {

        if (!id.equals(authenticatedOrganisationId)) {
            throw new ResourceNotFoundException("Organisation not found: " + id);
        }

        return organisationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Organisation not found: " + id));
    }
}