package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import za.co.bonalabs.bonahr.dto.employee.EmployeePageResponse;
import za.co.bonalabs.bonahr.dto.employee.*;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.EmployeeAuditService;
import za.co.bonalabs.bonahr.service.EmployeeDocumentService;
import za.co.bonalabs.bonahr.service.EmployeeService;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeAuditService employeeAuditService;
    private final EmployeeDocumentService employeeDocumentService;

    public EmployeeController(EmployeeService employeeService, EmployeeAuditService employeeAuditService, EmployeeDocumentService employeeDocumentService) {
        this.employeeService = employeeService;
        this.employeeAuditService = employeeAuditService;
        this.employeeDocumentService = employeeDocumentService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.createEmployee(details.organisationId(), request, details.userId());

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        URI location = URI.create("/api/v1/employees/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.getEmployee(details.organisationId(), id);

        return ResponseEntity.ok(EmployeeMapper.toResponse(employee));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String department,
            Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<Employee> employees = employeeService.filterEmployees(details.organisationId(), search, status, department);

        List<EmployeeResponse> response = employees.stream().map(EmployeeMapper::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable UUID id, @Valid @RequestBody UpdateEmployeeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.updateEmployee(details.organisationId(), id, request, details.userId());

        return ResponseEntity.ok(EmployeeMapper.toResponse(employee));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeResponse> updateEmployeeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request,
            Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Employee employee = employeeService.updateEmployeeStatus(details.organisationId(), id, request.status(), details.userId());

        return ResponseEntity.ok(EmployeeMapper.toResponse(employee));
    }

    @GetMapping("/paged")
    public ResponseEntity<EmployeePageResponse> getEmployeesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String department, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        Page<Employee> employeePage = employeeService.filterEmployeesPaged(details.organisationId(), search, status, department, PageRequest.of(page, size));

        List<EmployeeResponse> content = employeePage.getContent().stream().map(EmployeeMapper::toResponse).toList();

        EmployeePageResponse response = new EmployeePageResponse(
                content, employeePage.getTotalElements(), employeePage.getTotalPages(), employeePage.getNumber(), employeePage.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<EmployeeAuditResponse>> getEmployeeAuditHistory(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<EmployeeAuditLog> auditLogs = employeeAuditService.getEmployeeAuditHistory(details.organisationId(), id);

        List<EmployeeAuditResponse> response = auditLogs.stream().map(auditLog ->
                new EmployeeAuditResponse(
                        auditLog.getId(),
                        auditLog.getEmployee().getId(),
                        auditLog.getAction(),
                        auditLog.getActorUserId(),
                        auditLog.getChanges(),
                        auditLog.getCreatedAt())).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<List<EmployeeDocumentResponse>> getEmployeeDocuments(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<EmployeeDocument> documents = employeeDocumentService.getEmployeeDocuments(details.organisationId(), id);

        List<EmployeeDocumentResponse> response = documents.stream().map(document ->
                new EmployeeDocumentResponse(
                        document.getId(),
                        document.getEmployee().getId(),
                        document.getDocumentType(),
                        document.getFileName(),
                        document.getStorageKey(),
                        document.getMimeType(),
                        document.getFileSize(),
                        document.getUploadedByUserId(),
                        document.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmployeeDocumentResponse> uploadEmployeeDocument(
            @PathVariable UUID id,
            @RequestParam EmployeeDocumentType documentType,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() ? "document" : file.getOriginalFilename();

        EmployeeDocument document;

        try (InputStream inputStream = file.getInputStream()) {

            document = employeeDocumentService.uploadEmployeeDocument(
                    details.organisationId(),
                    id,
                    documentType,
                    fileName,
                    file.getContentType(),
                    file.getSize(),
                    inputStream,
                    details.userId());
        }

        EmployeeDocumentResponse response = new EmployeeDocumentResponse(
                document.getId(),
                document.getEmployee().getId(),
                document.getDocumentType(),
                document.getFileName(),
                document.getStorageKey(),
                document.getMimeType(),
                document.getFileSize(),
                document.getUploadedByUserId(),
                document.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{employeeId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadEmployeeDocument(@PathVariable UUID employeeId, @PathVariable UUID documentId, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        EmployeeDocument document = employeeDocumentService.getEmployeeDocument(details.organisationId(), employeeId, documentId);

        InputStream inputStream = employeeDocumentService.loadEmployeeDocument(details.organisationId(), employeeId, documentId);

        Resource resource = new InputStreamResource(inputStream);

        MediaType mediaType = document.getMimeType() == null
                || document.getMimeType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(document.getMimeType());

        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(document.getFileName()).build();

        return ResponseEntity.ok().contentType(mediaType).header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()).body(resource);
    }

    @DeleteMapping("/{employeeId}/documents/{documentId}")
    public ResponseEntity<Void> deleteEmployeeDocument(@PathVariable UUID employeeId, @PathVariable UUID documentId, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        employeeDocumentService.deleteEmployeeDocument(details.organisationId(), employeeId, documentId);

        return ResponseEntity.noContent().build();
    }
}