package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.dto.employee.UpdateEmployeeRequest;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.InvalidEmployeeStatusTransitionException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeAuditLogRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeServiceTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeAuditLogRepository employeeAuditLogRepository;

    @Test
    void shouldCreateEmployeeForOrganisation() {

        Organisation organisation = new Organisation("Employee Service Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        CreateEmployeeRequest request = new CreateEmployeeRequest("EMP-" + uniqueValue, "John", "Doe", "john-" + uniqueValue + "@example.com");

        Employee employee = employeeService.createEmployee(organisation.getId(), request);

        assertNotNull(employee.getId());

        assertEquals(organisation.getId(), employee.getOrganisation().getId());

        assertEquals(request.employeeNumber(), employee.getEmployeeNumber());

        assertEquals("John", employee.getFirstName());

        assertEquals("Doe", employee.getLastName());

        assertEquals(request.email(), employee.getEmail());

        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
    }

    @Test
    void shouldRejectEmployeeCreationForUnknownOrganisation() {

        UUID unknownOrganisationId = UUID.randomUUID();

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Jane", "Doe", "jane-" + UUID.randomUUID() + "@example.com");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(unknownOrganisationId, request));

        assertEquals("Organisation not found: " + unknownOrganisationId, exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmployeeNumberWithinSameOrganisation() {

        Organisation organisation = new Organisation("Duplicate Employee Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String employeeNumber = "EMP-001";

        CreateEmployeeRequest firstRequest = new CreateEmployeeRequest(
                employeeNumber, "John", "Doe", "john-" + UUID.randomUUID() + "@example.com");

        employeeService.createEmployee(organisation.getId(), firstRequest);

        CreateEmployeeRequest secondRequest = new CreateEmployeeRequest(
                employeeNumber, "Jane", "Doe", "jane-" + UUID.randomUUID() + "@example.com");

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(finalOrganisation.getId(), secondRequest));
    }

    @Test
    void shouldRejectDuplicateEmployeeEmailWithinSameOrganisation() {

        Organisation organisation = new Organisation("Duplicate Email Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String email = "employee-" + UUID.randomUUID() + "@example.com";

        CreateEmployeeRequest firstRequest = new CreateEmployeeRequest("EMP-001", "John", "Doe", email);

        employeeService.createEmployee(organisation.getId(), firstRequest);

        CreateEmployeeRequest secondRequest = new CreateEmployeeRequest("EMP-002", "Jane", "Doe", email.toUpperCase());

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(finalOrganisation.getId(), secondRequest));
    }

    @Test
    void shouldAllowSameEmployeeNumberAndEmailAcrossDifferentOrganisations() {

        Organisation organisationA = new Organisation("Organisation A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Organisation B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        String employeeNumber = "EMP-001";
        String email = "shared.employee@example.com";

        CreateEmployeeRequest requestA = new CreateEmployeeRequest(employeeNumber, "John", "Doe", email);

        CreateEmployeeRequest requestB = new CreateEmployeeRequest(employeeNumber, "Jane", "Doe", email);

        Employee employeeA = employeeService.createEmployee(organisationA.getId(), requestA);

        Employee employeeB = employeeService.createEmployee(organisationB.getId(), requestB);

        assertNotNull(employeeA.getId());
        assertNotNull(employeeB.getId());

        assertNotEquals(employeeA.getId(), employeeB.getId());

        assertEquals(organisationA.getId(), employeeA.getOrganisation().getId());

        assertEquals(organisationB.getId(), employeeB.getOrganisation().getId());
    }

    @Test
    void shouldReturnEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Lookup Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Lookup", "Employee", "lookup-" + UUID.randomUUID() + "@example.com"
        );

        Employee createdEmployee = employeeService.createEmployee(organisation.getId(), request);

        Employee retrievedEmployee = employeeService.getEmployee(organisation.getId(), createdEmployee.getId());

        assertEquals(createdEmployee.getId(), retrievedEmployee.getId());

        assertEquals(organisation.getId(), retrievedEmployee.getOrganisation().getId());

        assertEquals("Lookup", retrievedEmployee.getFirstName());
    }

    @Test
    void shouldNotReturnEmployeeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Tenant", "Employee", "tenant-" + UUID.randomUUID() + "@example.com"
        );

        Employee employee = employeeService.createEmployee(organisationA.getId(), request);

        Organisation finalOrganisationB = organisationB;
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.getEmployee(finalOrganisationB.getId(), employee.getId()));

        assertEquals("Employee not found: " + employee.getId(), exception.getMessage());
    }

    @Test
    void shouldReturnOnlyEmployeesForOrganisation() {

        Organisation organisationA = new Organisation("Employee List A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee List B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-A-001", "John", "Zulu", "john-a-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-A-002", "Jane", "Adams", "jane-a-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisationB.getId(), new CreateEmployeeRequest(
                "EMP-B-001", "Other", "Tenant", "other-" + UUID.randomUUID() + "@example.com"));

        List<Employee> employees = employeeService.getEmployees(organisationA.getId());

        assertEquals(2, employees.size());

        Organisation finalOrganisationA = organisationA;
        assertTrue(employees.stream().allMatch(employee -> employee.getOrganisation().getId().equals(finalOrganisationA.getId())));

        assertEquals("Adams", employees.get(0).getLastName());

        assertEquals("Zulu", employees.get(1).getLastName());
    }

    @Test
    void shouldUpdateEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Update Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        CreateEmployeeRequest createRequest = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "John", "Doe", "john-" + UUID.randomUUID() + "@example.com");

        Employee employee = employeeService.createEmployee(organisation.getId(), createRequest);

        String updatedEmail = "updated-" + UUID.randomUUID() + "@example.com";

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest("Johnny", "Smith", updatedEmail);

        Employee updatedEmployee = employeeService.updateEmployee(organisation.getId(), employee.getId(), updateRequest);

        assertEquals(employee.getId(), updatedEmployee.getId());

        assertEquals(organisation.getId(), updatedEmployee.getOrganisation().getId());

        assertEquals("Johnny", updatedEmployee.getFirstName());

        assertEquals("Smith", updatedEmployee.getLastName());

        assertEquals(updatedEmail, updatedEmployee.getEmail());

        // Employee number must remain unchanged.
        assertEquals(createRequest.employeeNumber(), updatedEmployee.getEmployeeNumber());

        // Normal profile editing must not change employment status.
        assertEquals(EmployeeStatus.ACTIVE, updatedEmployee.getStatus());
    }

    @Test
    void shouldNotUpdateEmployeeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Update Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Update Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Original", "Employee", "original-" + UUID.randomUUID() + "@example.com"));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Changed", "Employee", "changed-" + UUID.randomUUID() + "@example.com");

        Organisation finalOrganisationB = organisationB;
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.updateEmployee(finalOrganisationB.getId(), employee.getId(), updateRequest));

        assertEquals("Employee not found: " + employee.getId(), exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmployeeEmailDuringUpdate() {

        Organisation organisation = new Organisation("Employee Update Email Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String firstEmail = "first-" + UUID.randomUUID() + "@example.com";

        String secondEmail = "second-" + UUID.randomUUID() + "@example.com";

        employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-001-" + UUID.randomUUID(), "First", "Employee", firstEmail));

        Employee secondEmployee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-002-" + UUID.randomUUID(), "Second", "Employee", secondEmail));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(secondEmployee.getFirstName(), secondEmployee.getLastName(), firstEmail.toUpperCase());

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.updateEmployee(finalOrganisation.getId(), secondEmployee.getId(), updateRequest));
    }

    @Test
    void shouldDeactivateEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Status Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "John", "Doe", "john-" + UUID.randomUUID() + "@example.com"));

        Employee updatedEmployee = employeeService.updateEmployeeStatus(organisation.getId(), employee.getId(), EmployeeStatus.INACTIVE);

        assertEquals(EmployeeStatus.INACTIVE, updatedEmployee.getStatus());

        assertEquals(employee.getId(), updatedEmployee.getId());
    }

    @Test
    void shouldRejectTerminationThroughStatusUpdate() {

        Organisation organisation = new Organisation("Employee Termination Guard Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "John", "Doe", "john-" + UUID.randomUUID() + "@example.com"));

        Organisation finalOrganisation = organisation;
        InvalidEmployeeStatusTransitionException exception = assertThrows(InvalidEmployeeStatusTransitionException .class, () ->
                employeeService.updateEmployeeStatus(finalOrganisation.getId(), employee.getId(), EmployeeStatus.TERMINATED));

        assertEquals("Employee termination must use the termination workflow", exception.getMessage());

        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
    }

    @Test
    void shouldUpdateEmployeeEmploymentDetails() {

        Organisation organisation = new Organisation("Employment Details Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "John", "Doe", "john-" + UUID.randomUUID() + "@example.com"));

        LocalDate startDate = LocalDate.of(2026, 10, 1);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "John", "Doe", employee.getEmail(), "Senior Software Engineer", "Engineering",
                EmploymentType.PERMANENT, startDate, "+27 82 123 4567");

        Employee updatedEmployee = employeeService.updateEmployee(organisation.getId(), employee.getId(), request);

        assertEquals("Senior Software Engineer", updatedEmployee.getJobTitle());

        assertEquals("Engineering", updatedEmployee.getDepartment());

        assertEquals(EmploymentType.PERMANENT, updatedEmployee.getEmploymentType());

        assertEquals(startDate, updatedEmployee.getStartDate());

        assertEquals("+27 82 123 4567", updatedEmployee.getPhone());
    }

    @Test
    void shouldSearchEmployeesByNameWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Search Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-001-" + UUID.randomUUID(), "John", "Smith", "john-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-002-" + UUID.randomUUID(), "Jane", "Doe", "jane-" + UUID.randomUUID() + "@example.com"));

        List<Employee> employees = employeeService.searchEmployees(organisation.getId(), "john");

        assertEquals(1, employees.size());
        assertEquals("John", employees.getFirst().getFirstName());
        assertEquals("Smith", employees.getFirst().getLastName());
    }

    @Test
    void shouldNotReturnSearchResultsFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Search Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Search Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-A-" + UUID.randomUUID(), "John", "Smith", "john-a-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisationB.getId(), new CreateEmployeeRequest(
                "EMP-B-" + UUID.randomUUID(), "John", "OtherTenant", "john-b-" + UUID.randomUUID() + "@example.com"));

        List<Employee> employees = employeeService.searchEmployees(organisationA.getId(), "john");

        assertEquals(1, employees.size());

        assertEquals(organisationA.getId(), employees.getFirst().getOrganisation().getId());

        assertEquals("Smith", employees.getFirst().getLastName());
    }

    @Test
    void shouldReturnEmployeesByStatusWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Status Filter Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee activeEmployee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-A-" + UUID.randomUUID(), "Active", "Employee", "active-" + UUID.randomUUID() + "@example.com"));

        Employee inactiveEmployee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-I-" + UUID.randomUUID(), "Inactive", "Employee", "inactive-" + UUID.randomUUID() + "@example.com"));

        employeeService.updateEmployeeStatus(organisation.getId(), inactiveEmployee.getId(), EmployeeStatus.INACTIVE);

        List<Employee> employees = employeeService.getEmployeesByStatus(organisation.getId(), EmployeeStatus.ACTIVE);

        assertEquals(1, employees.size());

        assertEquals(activeEmployee.getId(), employees.getFirst().getId());

        assertEquals(EmployeeStatus.ACTIVE, employees.getFirst().getStatus());
    }

    @Test
    void shouldReturnEmployeesByDepartmentWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Department Filter Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee engineeringEmployee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-E-" + UUID.randomUUID(),
                "John",
                "Engineer",
                "john-" + UUID.randomUUID() + "@example.com",
                "Software Engineer",
                "Engineering",
                EmploymentType.PERMANENT,
                LocalDate.of(2026, 10, 1),
                "+27 82 111 1111"));

        employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-H-" + UUID.randomUUID(),
                "Jane",
                "HR",
                "jane-" + UUID.randomUUID() + "@example.com",
                "HR Manager",
                "Human Resources",
                EmploymentType.PERMANENT,
                LocalDate.of(2026, 10, 1),
                "+27 82 222 2222"));

        List<Employee> employees = employeeService.getEmployeesByDepartment(organisation.getId(), "engineering");

        assertEquals(1, employees.size());

        assertEquals(engineeringEmployee.getId(), employees.getFirst().getId());

        assertEquals("Engineering", employees.getFirst().getDepartment());
    }

    @Test
    void shouldReturnPaginatedEmployeesForOrganisation() {

        Organisation organisation = new Organisation("Employee Pagination Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        for (int i = 1; i <= 5; i++) {

            employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                    "EMP-" + i + "-" + UUID.randomUUID(),
                    "Employee" + i,
                    "User" + i,
                    "employee-" + i + "-" + UUID.randomUUID() + "@example.com"));
        }

        Page<Employee> page = employeeService.filterEmployeesPaged(organisation.getId(), null, null, null, PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(0, page.getNumber());
        assertEquals(2, page.getSize());
    }

    @Test
    void shouldCreateAuditLogWhenEmployeeIsUpdated() {

        Organisation organisation = new Organisation("Employee Update Audit Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(),
                "John",
                "Doe",
                "john-" + UUID.randomUUID() + "@example.com",
                "Software Engineer",
                "Engineering",
                EmploymentType.PERMANENT,
                LocalDate.of(2026, 10, 1),
                "+27 82 123 4567"));

        employeeAuditLogRepository.deleteAll();
        employeeAuditLogRepository.flush();

        UUID actorUserId = UUID.randomUUID();

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "Johnny",
                "Doe", employee.getEmail(),
                "Senior Software Engineer",
                "Engineering",
                EmploymentType.PERMANENT,
                employee.getStartDate(),
                employee.getPhone());

        employeeService.updateEmployee(organisation.getId(), employee.getId(), request, actorUserId);

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.EMPLOYEE_UPDATED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());

        assertTrue(auditLog.getChanges().containsKey("firstName"));

        assertTrue(auditLog.getChanges().containsKey("jobTitle"));
    }

    @Test
    void shouldCreateAuditLogWhenEmployeeStatusChanges() {

        Organisation organisation = new Organisation("Employee Status Audit Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(),
                "John",
                "Doe",
                "john-" + UUID.randomUUID() + "@example.com"));

        employeeAuditLogRepository.deleteAll();
        employeeAuditLogRepository.flush();

        UUID actorUserId = UUID.randomUUID();

        employeeService.updateEmployeeStatus(organisation.getId(), employee.getId(), EmployeeStatus.INACTIVE, actorUserId);

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.STATUS_CHANGED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());

        assertTrue(auditLog.getChanges().containsKey("status"));

        @SuppressWarnings("unchecked")
        Map<String, Object> statusChange = (Map<String, Object>) auditLog.getChanges().get("status");

        assertEquals("ACTIVE", statusChange.get("from"));

        assertEquals("INACTIVE", statusChange.get("to"));
    }

    @Test
    void shouldCreateAuditLogWhenEmployeeIsCreated() {

        Organisation organisation = new Organisation("Employee Creation Audit Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        UUID actorUserId = UUID.randomUUID();

        Employee employee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(),
                "John",
                "Doe",
                "john-" + UUID.randomUUID() + "@example.com"), actorUserId);

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.EMPLOYEE_CREATED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());

        assertEquals(employee.getId(), auditLog.getEmployee().getId());

        assertEquals(organisation.getId(), auditLog.getOrganisation().getId());
    }

}