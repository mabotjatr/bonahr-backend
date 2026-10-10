package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.LeaveType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    Optional<LeaveType> findByIdAndOrganisationId(UUID id, UUID organisationId);

    boolean existsByOrganisationIdAndCodeIgnoreCase(UUID organisationId, String code);

    List<LeaveType> findAllByOrganisationIdOrderByNameAsc(UUID organisationId);
}