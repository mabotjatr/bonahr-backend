package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;

import java.util.List;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {

    List<EmployeeDocument> findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(UUID organisationId, UUID employeeId);
}