package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.EmployeeLeaveAccount;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeLeaveAccountRepository extends JpaRepository<EmployeeLeaveAccount, UUID> {

    Optional<EmployeeLeaveAccount> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<EmployeeLeaveAccount> findByOrganisationIdAndEmployeeIdAndLeaveTypeId(UUID organisationId, UUID employeeId, UUID leaveTypeId);
}