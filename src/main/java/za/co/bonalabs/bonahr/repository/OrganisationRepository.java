package za.co.bonalabs.bonahr.repository;

import za.co.bonalabs.bonahr.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepository
        extends JpaRepository<Organisation, UUID> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByTaxNumber(String taxNumber);

    Optional<Organisation> findByName(String name);
}