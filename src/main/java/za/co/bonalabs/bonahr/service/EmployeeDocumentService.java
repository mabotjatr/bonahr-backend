package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;
import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeDocumentRepository;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.storage.FileStorageService;
import za.co.bonalabs.bonahr.storage.StoredFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeDocumentService {

    private final EmployeeService employeeService;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final FileStorageService fileStorageService;

    public EmployeeDocumentService(EmployeeService employeeService, EmployeeDocumentRepository employeeDocumentRepository, FileStorageService fileStorageService) {
        this.employeeService = employeeService;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocument> getEmployeeDocuments(UUID organisationId, UUID employeeId) {

        // Enforce tenant boundary first.
        employeeService.getEmployee(organisationId, employeeId);

        return employeeDocumentRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisationId, employeeId);
    }

    @Transactional
    public EmployeeDocument uploadEmployeeDocument(
            UUID organisationId,
            UUID employeeId,
            EmployeeDocumentType documentType,
            String fileName,
            String mimeType,
            long fileSize,
            InputStream inputStream,
            UUID uploadedByUserId) {

        Employee employee = employeeService.getEmployee(organisationId, employeeId);

        StoredFile storedFile = fileStorageService.store(organisationId, employeeId, fileName, mimeType, fileSize, inputStream);

        EmployeeDocument document = new EmployeeDocument(
                employee.getOrganisation(),
                employee,
                documentType,
                storedFile.fileName(),
                storedFile.storageKey(),
                storedFile.mimeType(),
                storedFile.fileSize(),
                uploadedByUserId);

        return employeeDocumentRepository.saveAndFlush(document);
    }

    @Transactional(readOnly = true)
    public InputStream loadEmployeeDocument(UUID organisationId, UUID employeeId, UUID documentId) {

        EmployeeDocument document = getEmployeeDocument(organisationId, employeeId, documentId);

        return fileStorageService.load(document.getStorageKey());
    }

    @Transactional(readOnly = true)
    public EmployeeDocument getEmployeeDocument(UUID organisationId, UUID employeeId, UUID documentId) {

        employeeService.getEmployee(organisationId, employeeId);

        return employeeDocumentRepository.findByIdAndOrganisationIdAndEmployeeId(
                documentId, organisationId, employeeId).orElseThrow(() -> new ResourceNotFoundException("Employee document not found: " + documentId));
    }

    @Transactional
    public void deleteEmployeeDocument(UUID organisationId, UUID employeeId, UUID documentId) {

        EmployeeDocument document = getEmployeeDocument(organisationId, employeeId, documentId);

        fileStorageService.delete(document.getStorageKey());

        employeeDocumentRepository.delete(document);
        employeeDocumentRepository.flush();
    }
}