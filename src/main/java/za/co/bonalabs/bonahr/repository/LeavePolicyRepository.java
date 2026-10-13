package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.LeavePolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, UUID> {

    Optional<LeavePolicy> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<LeavePolicy> findByOrganisationIdAndLeaveTypeId(UUID organisationId, UUID leaveTypeId);

    List<LeavePolicy> findAllByOrganisationId(UUID organisationId);
}