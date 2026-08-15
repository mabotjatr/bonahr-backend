package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.organisation.id = :organisationId
              and lower(u.email) = lower(:email)
            """)
    Optional<User> findByOrganisationIdAndEmailWithRoles(
            @Param("organisationId") UUID organisationId,
            @Param("email") String email
    );
}