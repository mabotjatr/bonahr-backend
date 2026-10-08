package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeDocumentRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;
import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.EmployeeDocumentRepository;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeDocumentServiceTest {

    @Autowired
    private EmployeeDocumentService employeeDocumentService;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldReturnDocumentsForEmployee() {

        Organisation organisation = new Organisation("Employee Document Service Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        EmployeeDocument document = new EmployeeDocument(
                organisation,
                employee,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                "employees/" + employee.getId() + "/" + UUID.randomUUID() + ".pdf",
                "application/pdf",
                75_000L,
                UUID.randomUUID());

        employeeDocumentRepository.saveAndFlush(document);

        List<EmployeeDocument> documents = employeeDocumentService.getEmployeeDocuments(organisation.getId(), employee.getId());

        assertEquals(1, documents.size());

        assertEquals(document.getId(), documents.getFirst().getId());
    }

    @Test
    void shouldNotAllowOrganisationToAccessAnotherOrganisationsEmployeeDocuments() {

        Organisation organisationA = new Organisation("Document Service Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Document Service Tenant B " + UUID.randomUUID());

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
                75_000L,
                UUID.randomUUID());

        employeeDocumentRepository.saveAndFlush(document);

        UUID organisationBId = organisationB.getId();
        UUID employeeId = employee.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.getEmployeeDocuments(organisationBId, employeeId));
    }

    @Test
    void shouldCreateEmployeeDocumentMetadata() {

        Organisation organisation = new Organisation("Employee Document Creation Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID uploadedByUserId = UUID.randomUUID();

        String storageKey = "employees/" + employee.getId() + "/" + UUID.randomUUID() + ".pdf";

        CreateEmployeeDocumentRequest request = new CreateEmployeeDocumentRequest(
                EmployeeDocumentType.EMPLOYMENT_CONTRACT, "employment-contract.pdf", storageKey, "application/pdf", 125_000L);

        EmployeeDocument document = employeeDocumentService.createEmployeeDocument(organisation.getId(), employee.getId(), request, uploadedByUserId);

        assertNotNull(document.getId());

        assertEquals(organisation.getId(), document.getOrganisation().getId());

        assertEquals(employee.getId(), document.getEmployee().getId());

        assertEquals(EmployeeDocumentType.EMPLOYMENT_CONTRACT, document.getDocumentType());

        assertEquals("employment-contract.pdf", document.getFileName());

        assertEquals(storageKey, document.getStorageKey());

        assertEquals("application/pdf", document.getMimeType());

        assertEquals(125_000L, document.getFileSize());

        assertEquals(uploadedByUserId, document.getUploadedByUserId());

        assertNotNull(document.getCreatedAt());
    }

    @Test
    void shouldNotAllowOrganisationToCreateDocumentForAnotherOrganisationsEmployee() {

        Organisation organisationA = new Organisation("Document Creation Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Document Creation Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        CreateEmployeeDocumentRequest request = new CreateEmployeeDocumentRequest(
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                "employees/" + employee.getId() + "/" + UUID.randomUUID() + ".pdf",
                "application/pdf",
                75_000L);

        UUID organisationBId = organisationB.getId();
        UUID employeeId = employee.getId();
        UUID uploadedByUserId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.createEmployeeDocument(organisationBId, employeeId, request, uploadedByUserId));

        List<EmployeeDocument> documents = employeeDocumentRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(
                organisationA.getId(),
                employee.getId());

        assertTrue(documents.isEmpty());
    }
}