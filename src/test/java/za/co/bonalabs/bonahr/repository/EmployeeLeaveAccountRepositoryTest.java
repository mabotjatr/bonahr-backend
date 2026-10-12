package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EmployeeLeaveAccountRepositoryTest {

    @Autowired
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Test
    void shouldPersistAndFindEmployeeLeaveAccountWithinOrganisation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account Repository Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        EmployeeLeaveAccount found = employeeLeaveAccountRepository.findByIdAndOrganisationId(account.getId(), organisation.getId()).orElseThrow();

        assertEquals(account.getId(), found.getId());

        assertEquals(employee.getId(), found.getEmployee().getId());

        assertEquals(leaveType.getId(), found.getLeaveType().getId());

        assertEquals(0, found.getCurrentBalance().compareTo(BigDecimal.ZERO));

        assertEquals(0, found.getReservedDays().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldFindAccountByOrganisationEmployeeAndLeaveType() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account Lookup Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(
                new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        EmployeeLeaveAccount found = employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(
                organisation.getId(),
                employee.getId(),
                leaveType.getId()).orElseThrow();

        assertEquals(account.getId(), found.getId());
    }

    @Test
    void shouldNotFindAccountFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Account Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Account Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationA, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisationA, employee, leaveType, leavePolicy));

        var result = employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationB.getId(), employee.getId(), leaveType.getId());

        assertTrue(result.isEmpty());
    }
}