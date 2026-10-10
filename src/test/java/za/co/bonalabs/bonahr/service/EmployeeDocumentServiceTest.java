package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;
import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.EmployeeDocumentRepository;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.storage.FileStorageService;
import za.co.bonalabs.bonahr.storage.StoredFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@SpringBootTest
@Transactional
class EmployeeDocumentServiceTest {

    @MockitoBean
    private FileStorageService fileStorageService;

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
    void shouldStoreFileAndCreateEmployeeDocumentMetadata() {

        Organisation organisation = new Organisation("Employee File Upload Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID uploadedByUserId = UUID.randomUUID();

        byte[] content = "BonaHR employment contract".getBytes(StandardCharsets.UTF_8);

        String generatedStorageKey = "organisations/" + organisation.getId() + "/employees/" + employee.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        when(fileStorageService.store(
                eq(organisation.getId()),
                eq(employee.getId()),
                eq("employment-contract.pdf"),
                eq("application/pdf"),
                eq((long) content.length),
                any())).thenReturn(new StoredFile(generatedStorageKey, "employment-contract.pdf", "application/pdf", content.length));

        EmployeeDocument document = employeeDocumentService.uploadEmployeeDocument(
                organisation.getId(),
                employee.getId(),
                EmployeeDocumentType.EMPLOYMENT_CONTRACT,
                "employment-contract.pdf",
                "application/pdf",
                content.length,
                new ByteArrayInputStream(content),
                uploadedByUserId);

        assertNotNull(document.getId());

        assertEquals(generatedStorageKey, document.getStorageKey());

        assertEquals("employment-contract.pdf", document.getFileName());

        assertEquals("application/pdf", document.getMimeType());

        assertEquals(content.length, document.getFileSize());

        assertEquals(uploadedByUserId, document.getUploadedByUserId());
    }

    @Test
    void shouldLoadEmployeeDocumentFile() throws Exception {

        Organisation organisation = new Organisation("Employee Document Download Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String storageKey = "organisations/" + organisation.getId() + "/employees/" + employee.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisation, employee,
                EmployeeDocumentType.EMPLOYMENT_CONTRACT,
                "employment-contract.pdf",
                storageKey,
                "application/pdf",
                125_000L,
                UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        byte[] content = "BonaHR employment contract".getBytes(StandardCharsets.UTF_8);

        when(fileStorageService.load(storageKey)).thenReturn(new ByteArrayInputStream(content));

        InputStream inputStream = employeeDocumentService.loadEmployeeDocument(organisation.getId(), employee.getId(), document.getId());

        try (inputStream) {
            assertArrayEquals(content, inputStream.readAllBytes());
        }
    }

    @Test
    void shouldNotAllowOrganisationToLoadAnotherOrganisationsEmployeeDocument() throws Exception {

        Organisation organisationA = new Organisation("Document Download Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Document Download Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String storageKey = "organisations/" + organisationA.getId() + "/employees/" + employee.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisationA,
                employee,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                storageKey,
                "application/pdf",
                80_000L,
                UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        UUID organisationBId = organisationB.getId();
        UUID employeeId = employee.getId();
        UUID documentId = document.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.loadEmployeeDocument(organisationBId, employeeId, documentId));
    }

    @Test
    void shouldNotAllowEmployeeToLoadAnotherEmployeesDocumentWithinSameOrganisation() throws Exception {

        Organisation organisation = new Organisation("Document Employee Isolation Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employeeA = new Employee(organisation, "EMP-A-" + UUID.randomUUID(), "John", "Doe");

        Employee employeeB = new Employee(organisation, "EMP-B-" + UUID.randomUUID(), "Jane", "Smith");

        employeeA = employeeRepository.saveAndFlush(employeeA);

        employeeB = employeeRepository.saveAndFlush(employeeB);

        String storageKey = "organisations/" + organisation.getId() + "/employees/" + employeeB.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(organisation, employeeB, EmployeeDocumentType.ID_DOCUMENT, "id-document.pdf", storageKey, "application/pdf", 80_000L, UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        UUID organisationId = organisation.getId();
        UUID employeeAId = employeeA.getId();
        UUID documentId = document.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.loadEmployeeDocument(organisationId, employeeAId, documentId));
    }

    @Test
    void shouldDeleteEmployeeDocument() {

        Organisation organisation = new Organisation("Employee Document Delete Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String storageKey = "organisations/" + organisation.getId() + "/employees/" + employee.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisation,
                employee,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                storageKey,
                "application/pdf",
                80_000L,
                UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        UUID documentId = document.getId();

        employeeDocumentService.deleteEmployeeDocument(organisation.getId(), employee.getId(), documentId);

        assertFalse(employeeDocumentRepository.existsById(documentId));

        verify(fileStorageService).delete(storageKey);
    }

    @Test
    void shouldNotAllowOrganisationToDeleteAnotherOrganisationsEmployeeDocument() {

        Organisation organisationA = new Organisation("Document Delete Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Document Delete Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String storageKey = "organisations/" + organisationA.getId() + "/employees/" + employee.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisationA,
                employee,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                storageKey,
                "application/pdf",
                80_000L,
                UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        UUID organisationBId = organisationB.getId();
        UUID employeeId = employee.getId();
        UUID documentId = document.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.deleteEmployeeDocument(organisationBId, employeeId, documentId));

        assertTrue(employeeDocumentRepository.existsById(documentId));

        verify(fileStorageService, never()).delete(storageKey);
    }

    @Test
    void shouldNotAllowEmployeeToDeleteAnotherEmployeesDocumentWithinSameOrganisation() {

        Organisation organisation = new Organisation("Document Delete Employee Isolation " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employeeA = new Employee(organisation, "EMP-A-" + UUID.randomUUID(), "John", "Doe");

        Employee employeeB = new Employee(organisation, "EMP-B-" + UUID.randomUUID(), "Jane", "Smith");

        employeeA = employeeRepository.saveAndFlush(employeeA);

        employeeB = employeeRepository.saveAndFlush(employeeB);

        String storageKey = "organisations/" + organisation.getId() + "/employees/" + employeeB.getId() + "/documents/" + UUID.randomUUID() + ".pdf";

        EmployeeDocument document = new EmployeeDocument(
                organisation,
                employeeB,
                EmployeeDocumentType.ID_DOCUMENT,
                "id-document.pdf",
                storageKey,
                "application/pdf",
                80_000L,
                UUID.randomUUID());

        document = employeeDocumentRepository.saveAndFlush(document);

        UUID organisationId = organisation.getId();
        UUID employeeAId = employeeA.getId();
        UUID documentId = document.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeDocumentService.deleteEmployeeDocument(organisationId, employeeAId, documentId));

        assertTrue(employeeDocumentRepository.existsById(documentId));

        verify(fileStorageService, never()).delete(storageKey);
    }
}