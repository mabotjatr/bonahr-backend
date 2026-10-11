package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.LeaveRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndOrganisationId(UUID id, UUID organisationId);

    List<LeaveRequest> findAllByOrganisationIdOrderByStartDateAsc(UUID organisationId);
}