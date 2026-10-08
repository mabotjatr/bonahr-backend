package za.co.bonalabs.bonahr.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.dto.employee.UpdateEmployeeRequest;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.InvalidEmployeeStatusTransitionException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeAuditLogRepository;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.*;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganisationRepository organisationRepository;
    private final EmployeeAuditLogRepository employeeAuditLogRepository;

    public EmployeeService(EmployeeRepository employeeRepository, OrganisationRepository organisationRepository,EmployeeAuditLogRepository employeeAuditLogRepository) {
        this.employeeRepository = employeeRepository;
        this.organisationRepository = organisationRepository;
        this.employeeAuditLogRepository = employeeAuditLogRepository;
    }

    @Transactional
    public Employee createEmployee(UUID organisationId, CreateEmployeeRequest request) {
        return createEmployee(organisationId, request, null);
    }

    @Transactional
    public Employee createEmployee(UUID organisationId, CreateEmployeeRequest request, UUID actorUserId) {

        Organisation organisation = organisationRepository.findById(organisationId).orElseThrow(() -> new ResourceNotFoundException("Organisation not found: " + organisationId));

        if (employeeRepository.existsByOrganisationIdAndEmployeeNumber(organisationId, request.employeeNumber())) {

            throw new DuplicateResourceException("Employee number already exists: " + request.employeeNumber());
        }

        if (request.email() != null && !request.email().isBlank() && employeeRepository.existsByOrganisationIdAndEmailIgnoreCase(organisationId, request.email())) {

            throw new DuplicateResourceException("Employee email already exists: " + request.email());
        }

        Employee employee = new Employee(organisation, request.employeeNumber(), request.firstName(), request.lastName());

        employee.setEmail(request.email());
        employee.setJobTitle(request.jobTitle());
        employee.setDepartment(request.department());
        employee.setEmploymentType(request.employmentType());
        employee.setStartDate(request.startDate());
        employee.setPhone(request.phone());

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);

        EmployeeAuditLog auditLog = new EmployeeAuditLog(organisation, savedEmployee, EmployeeAuditAction.EMPLOYEE_CREATED, actorUserId, null);

        employeeAuditLogRepository.saveAndFlush(auditLog);

        return savedEmployee;
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(UUID organisationId, UUID employeeId) {

        return employeeRepository.findByIdAndOrganisationId(employeeId, organisationId).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployees(UUID organisationId) {
        return employeeRepository
                .findAllByOrganisationIdOrderByLastNameAscFirstNameAsc( organisationId);
    }

    public Employee updateEmployee(UUID organisationId, UUID employeeId, UpdateEmployeeRequest request) {
        return updateEmployee(organisationId, employeeId, request, null);
    }

    public Employee updateEmployee(UUID organisationId, UUID employeeId, UpdateEmployeeRequest request, UUID actorUserId) {

        Employee employee = getEmployee(organisationId, employeeId);

        if (request.email() != null
                && !request.email().isBlank()
                && !request.email().equalsIgnoreCase(employee.getEmail())
                && employeeRepository.existsByOrganisationIdAndEmailIgnoreCase(organisationId, request.email())) {

            throw new DuplicateResourceException("Employee email already exists: " + request.email());
        }

        Map<String, Object> changes = new LinkedHashMap<>();

        recordChange(changes, "firstName", employee.getFirstName(), request.firstName());

        recordChange(changes, "lastName", employee.getLastName(), request.lastName());

        recordChange(changes, "email", employee.getEmail(), request.email());

        recordChange(changes, "jobTitle", employee.getJobTitle(), request.jobTitle());

        recordChange(changes, "department", employee.getDepartment(), request.department());

        recordChange(changes, "employmentType", employee.getEmploymentType(), request.employmentType());

        recordChange(changes, "startDate", employee.getStartDate(), request.startDate());

        recordChange(changes, "phone", employee.getPhone(), request.phone());

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setJobTitle(request.jobTitle());
        employee.setDepartment(request.department());
        employee.setEmploymentType(request.employmentType());
        employee.setStartDate(request.startDate());
        employee.setPhone(request.phone());

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);

        if (!changes.isEmpty()) {

            EmployeeAuditLog auditLog = new EmployeeAuditLog(employee.getOrganisation(), savedEmployee, EmployeeAuditAction.EMPLOYEE_UPDATED, actorUserId, changes);

            employeeAuditLogRepository.saveAndFlush(auditLog);
        }

        return savedEmployee;
    }

    private void recordChange(Map<String, Object> changes, String field, Object oldValue, Object newValue) {

        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        Map<String, Object> change = new LinkedHashMap<>();

        change.put("from", oldValue);
        change.put("to", newValue);

        changes.put(field, change);
    }

    public Employee updateEmployeeStatus(UUID organisationId, UUID employeeId, EmployeeStatus status) {
        return updateEmployeeStatus(organisationId, employeeId, status, null);
    }

    public Employee updateEmployeeStatus(UUID organisationId, UUID employeeId, EmployeeStatus status, UUID actorUserId) {

        Employee employee = getEmployee(organisationId, employeeId);

        if (status == EmployeeStatus.TERMINATED) {
            throw new InvalidEmployeeStatusTransitionException("Employee termination must use the termination workflow");
        }

        EmployeeStatus previousStatus = employee.getStatus();

        employee.setStatus(status);

        Employee savedEmployee = employeeRepository.saveAndFlush(employee);

        if (previousStatus != status) {

            Map<String, Object> statusChange = new LinkedHashMap<>();

            statusChange.put("from", previousStatus != null ? previousStatus.name() : null);

            statusChange.put("to", status != null ? status.name() : null);

            Map<String, Object> changes = new LinkedHashMap<>();

            changes.put("status", statusChange);

            EmployeeAuditLog auditLog = new EmployeeAuditLog(employee.getOrganisation(), savedEmployee, EmployeeAuditAction.STATUS_CHANGED, actorUserId, changes);

            employeeAuditLogRepository.saveAndFlush(auditLog);
        }

        return savedEmployee;
    }

    @Transactional(readOnly = true)
    public List<Employee> searchEmployees(UUID organisationId, String search) {
        return employeeRepository
                .findAllByOrganisationIdAndFirstNameContainingIgnoreCaseOrOrganisationIdAndLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
                        organisationId,
                        search,
                        organisationId,
                        search
                );
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByStatus(UUID organisationId, EmployeeStatus status) {
        return employeeRepository
                .findAllByOrganisationIdAndStatusOrderByLastNameAscFirstNameAsc(
                        organisationId,
                        status
                );
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByDepartment(UUID organisationId, String department) {
        return employeeRepository.findAllByOrganisationIdAndDepartmentIgnoreCaseOrderByLastNameAscFirstNameAsc(organisationId, department);
    }

    @Transactional(readOnly = true)
    public List<Employee> filterEmployees(UUID organisationId, String search, EmployeeStatus status, String department) {

        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim().toLowerCase(Locale.ROOT);

        String normalizedDepartment = department == null || department.isBlank() ? "" : department.trim().toLowerCase(Locale.ROOT);

        return employeeRepository.findAllByFilters(organisationId, normalizedSearch, status, normalizedDepartment);
    }

    @Transactional(readOnly = true)
    public Page<Employee> filterEmployeesPaged(UUID organisationId, String search, EmployeeStatus status, String department, Pageable pageable) {

        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim().toLowerCase(Locale.ROOT);

        String normalizedDepartment = department == null || department.isBlank() ? "" : department.trim().toLowerCase(Locale.ROOT);

        return employeeRepository.findAllByFiltersPaged(organisationId, normalizedSearch, status, normalizedDepartment, pageable);
    }
}