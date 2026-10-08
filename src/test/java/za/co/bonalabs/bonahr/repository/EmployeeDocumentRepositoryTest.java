package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;
import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;
import za.co.bonalabs.bonahr.entity.Organisation;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeDocumentRepositoryTest {

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldPersistEmployeeDocumentMetadata() {

        Organisation organisation = new Organisation("Employee Document Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID uploadedByUserId = UUID.randomUUID();

        String storageKey = "employees/" + employee.getId() + "/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisation,
                employee,
                EmployeeDocumentType.EMPLOYMENT_CONTRACT,
                "employment-contract.pdf",
                storageKey,
                "application/pdf",
                125_000L,
                uploadedByUserId);

        employeeDocumentRepository.saveAndFlush(document);

        List<EmployeeDocument> documents = employeeDocumentRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, documents.size());

        EmployeeDocument persisted = documents.getFirst();

        assertNotNull(persisted.getId());

        assertEquals(organisation.getId(), persisted.getOrganisation().getId());

        assertEquals(employee.getId(), persisted.getEmployee().getId());

        assertEquals(EmployeeDocumentType.EMPLOYMENT_CONTRACT, persisted.getDocumentType());

        assertEquals("employment-contract.pdf", persisted.getFileName());

        assertEquals(storageKey, persisted.getStorageKey());

        assertEquals("application/pdf", persisted.getMimeType());

        assertEquals(125_000L, persisted.getFileSize());

        assertEquals(uploadedByUserId, persisted.getUploadedByUserId());

        assertNotNull(persisted.getCreatedAt());
    }

    @Test
    void shouldNotReturnEmployeeDocumentsFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Document Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Document Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        EmployeeDocument document = new EmployeeDocument(
                organisationA,
                employee,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                "employees/" + employee.getId() + "/" + UUID.randomUUID() + ".pdf",
                "application/pdf",
                80_000L, UUID.randomUUID());

        employeeDocumentRepository.saveAndFlush(document);

        List<EmployeeDocument> documents = employeeDocumentRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisationB.getId(), employee.getId());

        assertTrue(documents.isEmpty());
    }
}