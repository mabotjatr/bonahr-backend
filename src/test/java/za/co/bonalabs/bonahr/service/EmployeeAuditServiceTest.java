package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeAuditAction;
import za.co.bonalabs.bonahr.entity.EmployeeAuditLog;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeAuditLogRepository;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class EmployeeAuditServiceTest {

    @Autowired
    private EmployeeAuditService employeeAuditService;

    @Autowired
    private EmployeeAuditLogRepository employeeAuditLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldReturnAuditHistoryForEmployee() {

        Organisation organisation = new Organisation("Employee Audit History Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        EmployeeAuditLog createdLog = new EmployeeAuditLog(organisation, employee, EmployeeAuditAction.EMPLOYEE_CREATED, UUID.randomUUID(), null);

        EmployeeAuditLog updatedLog = new EmployeeAuditLog(
                organisation,
                employee,
                EmployeeAuditAction.EMPLOYEE_UPDATED,
                UUID.randomUUID(),
                Map.of("jobTitle",
                        Map.of("from", "Developer", "to", "Senior Developer")));

        employeeAuditLogRepository.saveAndFlush(createdLog);
        employeeAuditLogRepository.saveAndFlush(updatedLog);

        List<EmployeeAuditLog> auditHistory = employeeAuditService.getEmployeeAuditHistory(organisation.getId(), employee.getId());

        assertEquals(2, auditHistory.size());
    }

    @Test
    void shouldNotAllowOrganisationToAccessAnotherOrganisationsEmployeeAuditHistory() {

        Organisation organisationA = new Organisation("Audit Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Audit Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        EmployeeAuditLog auditLog = new EmployeeAuditLog(organisationA, employee, EmployeeAuditAction.EMPLOYEE_CREATED, UUID.randomUUID(), null);

        employeeAuditLogRepository.saveAndFlush(auditLog);

        UUID organisationBId = organisationB.getId();
        UUID employeeId = employee.getId();

        assertThrows(ResourceNotFoundException.class, () -> employeeAuditService.getEmployeeAuditHistory(organisationBId, employeeId));
    }
}