package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.EmployeeAuditLog;
import za.co.bonalabs.bonahr.repository.EmployeeAuditLogRepository;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeAuditService {

    private final EmployeeService employeeService;
    private final EmployeeAuditLogRepository employeeAuditLogRepository;

    public EmployeeAuditService(EmployeeService employeeService, EmployeeAuditLogRepository employeeAuditLogRepository) {
        this.employeeService = employeeService;
        this.employeeAuditLogRepository = employeeAuditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeAuditLog> getEmployeeAuditHistory(UUID organisationId, UUID employeeId) {

        /*
         * Important tenant boundary:
         * verify the employee belongs to this organisation first.
         *
         * EmployeeService#getEmployee already performs:
         * findByIdAndOrganisationId(...)
         * and throws ResourceNotFoundException when not found.
         */
        employeeService.getEmployee(organisationId, employeeId);

        return employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisationId, employeeId);
    }
}