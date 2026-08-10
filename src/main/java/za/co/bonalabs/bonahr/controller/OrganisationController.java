package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.dto.organisation.CreateOrganisationRequest;
import za.co.bonalabs.bonahr.dto.organisation.OrganisationMapper;
import za.co.bonalabs.bonahr.dto.organisation.OrganisationResponse;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.service.OrganisationService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisations")
public class OrganisationController {

    private final OrganisationService organisationService;

    public OrganisationController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @PostMapping
    public ResponseEntity<OrganisationResponse> createOrganisation(
            @Valid @RequestBody CreateOrganisationRequest request
    ) {

        Organisation organisation =
                organisationService.createOrganisation(request);

        OrganisationResponse response =
                OrganisationMapper.toResponse(organisation);

        URI location = URI.create(
                "/api/v1/organisations/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganisationResponse> getOrganisation(
            @PathVariable UUID id
    ) {

        Organisation organisation =
                organisationService.getOrganisation(id);

        return ResponseEntity.ok(
                OrganisationMapper.toResponse(organisation)
        );
    }
}