package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeAuditAction;
import za.co.bonalabs.bonahr.entity.EmployeeAuditLog;
import za.co.bonalabs.bonahr.entity.Organisation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeAuditLogRepositoryTest {

    @Autowired
    private EmployeeAuditLogRepository employeeAuditLogRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldPersistEmployeeAuditLog() {

        Organisation organisation = new Organisation("Employee Audit Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID actorUserId = UUID.randomUUID();

        Map<String, Object> changes = Map.of("department", Map.of("from", "Engineering", "to", "Architecture"));

        EmployeeAuditLog auditLog = new EmployeeAuditLog(organisation, employee, EmployeeAuditAction.EMPLOYEE_UPDATED, actorUserId, changes);

        employeeAuditLogRepository.saveAndFlush(auditLog);

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog persistedAuditLog = auditLogs.getFirst();

        assertNotNull(persistedAuditLog.getId());

        assertEquals(organisation.getId(), persistedAuditLog.getOrganisation().getId());

        assertEquals(employee.getId(), persistedAuditLog.getEmployee().getId());

        assertEquals(EmployeeAuditAction.EMPLOYEE_UPDATED, persistedAuditLog.getAction());

        assertEquals(actorUserId, persistedAuditLog.getActorUserId());

        assertNotNull(persistedAuditLog.getChanges());

        assertTrue(persistedAuditLog.getChanges().containsKey("department"));

        assertNotNull(persistedAuditLog.getCreatedAt());
    }

    @Test
    void shouldNotReturnAuditLogsFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Audit Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Audit Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        EmployeeAuditLog auditLog = new EmployeeAuditLog(
                organisationA,
                employee,
                EmployeeAuditAction.EMPLOYEE_UPDATED,
                UUID.randomUUID(),
                Map.of("jobTitle", Map.of("from", "Developer", "to", "Senior Developer")));

        employeeAuditLogRepository.saveAndFlush(auditLog);

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisationB.getId(), employee.getId());

        assertTrue(auditLogs.isEmpty());
    }
}