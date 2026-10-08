package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.EmployeeDocument;
import za.co.bonalabs.bonahr.repository.EmployeeDocumentRepository;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeDocumentRequest;
import za.co.bonalabs.bonahr.entity.Employee;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeDocumentService {

    private final EmployeeService employeeService;
    private final EmployeeDocumentRepository employeeDocumentRepository;

    public EmployeeDocumentService(EmployeeService employeeService, EmployeeDocumentRepository employeeDocumentRepository) {
        this.employeeService = employeeService;
        this.employeeDocumentRepository = employeeDocumentRepository;
    }

    @Transactional
    public EmployeeDocument createEmployeeDocument(UUID organisationId, UUID employeeId, CreateEmployeeDocumentRequest request, UUID uploadedByUserId) {

        Employee employee = employeeService.getEmployee(organisationId, employeeId);

        EmployeeDocument document = new EmployeeDocument(
                employee.getOrganisation(),
                employee,
                request.documentType(),
                request.fileName(),
                request.storageKey(),
                request.mimeType(),
                request.fileSize(),
                uploadedByUserId);

        return employeeDocumentRepository.saveAndFlush(document);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocument> getEmployeeDocuments(UUID organisationId, UUID employeeId) {

        // Enforce tenant boundary first.
        employeeService.getEmployee(organisationId, employeeId);

        return employeeDocumentRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisationId, employeeId);
    }
}