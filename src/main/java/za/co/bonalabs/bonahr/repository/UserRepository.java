package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByOrganisationIdAndEmail(
            UUID organisationId,
            String email
    );

    boolean existsByOrganisationIdAndEmail(
            UUID organisationId,
            String email
    );
}