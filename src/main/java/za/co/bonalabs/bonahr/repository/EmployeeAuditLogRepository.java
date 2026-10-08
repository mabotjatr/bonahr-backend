package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.EmployeeAuditLog;

import java.util.List;
import java.util.UUID;

public interface EmployeeAuditLogRepository extends JpaRepository<EmployeeAuditLog, UUID> {

    List<EmployeeAuditLog> findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(UUID organisationId, UUID employeeId);
}