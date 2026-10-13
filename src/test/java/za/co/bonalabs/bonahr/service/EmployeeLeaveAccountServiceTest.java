package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.bonalabs.bonahr.dto.leave.CreateEmployeeLeaveAccountRequest;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;
import za.co.bonalabs.bonahr.repository.LeavePolicyRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeLeaveAccountServiceTest {

    @Mock
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeLeaveAccountService employeeLeaveAccountService;

    @Test
    void shouldCreateEmployeeLeaveAccount() {

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        Organisation organisation = new Organisation("Leave Account Company");

        Employee employee = new Employee(organisation, "EMP-001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        CreateEmployeeLeaveAccountRequest request = new CreateEmployeeLeaveAccountRequest(employeeId, leaveTypeId);

        when(employeeService.getEmployee(organisationId, employeeId)).thenReturn(employee);

        when(leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisationId, leaveTypeId)).thenReturn(Optional.of(leavePolicy));

        when(employeeLeaveAccountRepository.saveAndFlush(any(EmployeeLeaveAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeLeaveAccount result = employeeLeaveAccountService.createEmployeeLeaveAccount(organisationId, request);

        assertSame(organisation, result.getOrganisation());

        assertSame(employee, result.getEmployee());

        assertSame(leaveType, result.getLeaveType());

        assertSame(leavePolicy, result.getLeavePolicy());

        assertEquals(0, result.getCurrentBalance().compareTo(BigDecimal.ZERO));

        assertEquals(0, result.getReservedDays().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectDuplicateEmployeeLeaveAccount() {

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        Organisation organisation = new Organisation("Duplicate Leave Account Company");

        Employee employee = new Employee(organisation, "EMP-001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        EmployeeLeaveAccount existingAccount = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        CreateEmployeeLeaveAccountRequest request = new CreateEmployeeLeaveAccountRequest(employeeId, leaveTypeId);

        when(employeeService.getEmployee(organisationId, employeeId)).thenReturn(employee);

        when(leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisationId, leaveTypeId)).thenReturn(Optional.of(leavePolicy));

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.of(existingAccount));

        assertThrows(IllegalStateException.class, () -> employeeLeaveAccountService.createEmployeeLeaveAccount(organisationId, request));
    }

    @Test
    void shouldRejectAccountCreationWhenLeavePolicyDoesNotExist() {

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        Organisation organisation = new Organisation("Missing Policy Company");

        Employee employee = new Employee(organisation, "EMP-001", "John", "Doe");

        CreateEmployeeLeaveAccountRequest request = new CreateEmployeeLeaveAccountRequest(employeeId, leaveTypeId);

        when(employeeService.getEmployee(organisationId, employeeId)).thenReturn(employee);

        when(leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisationId, leaveTypeId)).thenReturn(Optional.empty());

        assertThrows(za.co.bonalabs.bonahr.exception.ResourceNotFoundException.class, () -> employeeLeaveAccountService.createEmployeeLeaveAccount(organisationId, request));
    }

    @Test
    void shouldGetEmployeeLeaveAccountById() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        Organisation organisation = new Organisation("Get Leave Account Company");

        Employee employee = new Employee(organisation, "EMP-001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        EmployeeLeaveAccount result = employeeLeaveAccountService.getEmployeeLeaveAccount(organisationId, accountId);

        assertSame(account, result);
    }

    @Test
    void shouldThrowWhenEmployeeLeaveAccountNotFoundForOrganisation() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.empty());

        assertThrows(za.co.bonalabs.bonahr.exception.ResourceNotFoundException.class, () -> employeeLeaveAccountService.getEmployeeLeaveAccount(organisationId, accountId));
    }

    @Test
    void shouldListEmployeeLeaveAccountsForOrganisation() {

        UUID organisationId = UUID.randomUUID();

        Organisation organisation = new Organisation("Leave Account List Company");

        Employee employeeOne = new Employee(organisation, "EMP-001", "John", "Doe");

        Employee employeeTwo = new Employee(organisation, "EMP-002", "Jane", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        EmployeeLeaveAccount accountOne = new EmployeeLeaveAccount(organisation, employeeOne, leaveType, leavePolicy);

        EmployeeLeaveAccount accountTwo = new EmployeeLeaveAccount(organisation, employeeTwo, leaveType, leavePolicy);

        when(employeeLeaveAccountRepository.findAllByOrganisationId(organisationId)).thenReturn(List.of(accountOne, accountTwo));

        java.util.List<EmployeeLeaveAccount> result = employeeLeaveAccountService.getEmployeeLeaveAccounts(organisationId);

        assertEquals(2, result.size());

        assertSame(accountOne, result.get(0));

        assertSame(accountTwo, result.get(1));
    }
}