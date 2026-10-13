package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;
import za.co.bonalabs.bonahr.repository.LeaveBalanceTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Mock
    private LeaveBalanceTransactionRepository leaveBalanceTransactionRepository;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    @Test
    void shouldApplyAccrualAndCreateLedgerTransaction() {

        Organisation organisation = new Organisation("Leave Balance Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        LocalDate accrualDate = LocalDate.of(2026, 10, 31);

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        leaveBalanceService.applyAccrual(organisationId, accountId, new BigDecimal("1.2500"), accrualDate, actorUserId);

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("1.2500")));

        assertEquals(accrualDate, account.getLastAccrualDate());

        verify(employeeLeaveAccountRepository).saveAndFlush(account);

        ArgumentCaptor<LeaveBalanceTransaction> captor = ArgumentCaptor.forClass(LeaveBalanceTransaction.class);

        verify(leaveBalanceTransactionRepository).saveAndFlush(captor.capture());

        LeaveBalanceTransaction transaction = captor.getValue();

        assertEquals(LeaveBalanceTransactionType.ACCRUAL, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("1.2500")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(new BigDecimal("1.2500")));

        assertEquals(0, transaction.getReservedAfter().compareTo(BigDecimal.ZERO));

        assertEquals(actorUserId, transaction.getActorUserId());
    }

    @Test
    void shouldRejectAccrualForLeaveAccountOutsideOrganisation() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveBalanceService.applyAccrual(
                        organisationId,
                accountId,
                new BigDecimal("1.2500"),
                LocalDate.of(2026, 10, 31),
                UUID.randomUUID()));

        verify(employeeLeaveAccountRepository, never()).saveAndFlush(any());

        verify(leaveBalanceTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReserveLeaveDaysAndCreateLedgerTransaction() {

        Organisation organisation = new Organisation("Leave Reservation Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        UUID requestedByUserId = UUID.randomUUID();

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                employee,
                leaveType,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave",
                requestedByUserId);

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        leaveBalanceService.reserveLeaveDays(organisationId, accountId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        assertEquals(0, account.getReservedDays().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);

        ArgumentCaptor<LeaveBalanceTransaction> captor = ArgumentCaptor.forClass(LeaveBalanceTransaction.class);

        verify(leaveBalanceTransactionRepository).saveAndFlush(captor.capture());

        LeaveBalanceTransaction transaction = captor.getValue();

        assertEquals(LeaveBalanceTransactionType.LEAVE_RESERVED, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(BigDecimal.ZERO));

        assertEquals(0, transaction.getReservedAfter().compareTo(new BigDecimal("2.0000")));

        assertEquals(leaveRequest, transaction.getLeaveRequest());

        assertEquals(actorUserId, transaction.getActorUserId());
    }

    @Test
    void shouldRejectReservationForDifferentEmployeeAccount() {

        Organisation organisation = new Organisation("Leave Account Employee Validation Company");

        Employee accountEmployee = new Employee(organisation, "EMP001", "John", "Doe");

        Employee requestEmployee = new Employee(organisation, "EMP002", "Jane", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, accountEmployee, leaveType, leavePolicy);

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                requestEmployee,
                leaveType,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave",
                UUID.randomUUID());

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> leaveBalanceService.reserveLeaveDays(organisationId, accountId, leaveRequest, new BigDecimal("1.0000"), UUID.randomUUID()));

        verify(employeeLeaveAccountRepository, never()).saveAndFlush(any());

        verify(leaveBalanceTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectReservationForDifferentLeaveTypeAccount() {

        Organisation organisation = new Organisation("Leave Type Validation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType annualLeave = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeaveType sickLeave = new LeaveType(organisation, "SICK", "Sick Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                annualLeave,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, annualLeave, leavePolicy);

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                employee,
                sickLeave,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Sick leave",
                UUID.randomUUID());

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> leaveBalanceService.reserveLeaveDays(
                organisationId,
                accountId,
                leaveRequest,
                new BigDecimal("1.0000"),
                UUID.randomUUID()));

        verify(employeeLeaveAccountRepository, never()).saveAndFlush(any());

        verify(leaveBalanceTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldReleaseReservedLeaveDaysAndCreateLedgerTransaction() {

        Organisation organisation = new Organisation("Leave Release Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.reserveDays(new BigDecimal("2.0000"));

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                employee,
                leaveType,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave",
                UUID.randomUUID());

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        leaveBalanceService.releaseLeaveDays(organisationId, accountId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);

        ArgumentCaptor<LeaveBalanceTransaction> captor = ArgumentCaptor.forClass(LeaveBalanceTransaction.class);

        verify(leaveBalanceTransactionRepository).saveAndFlush(captor.capture());

        LeaveBalanceTransaction transaction = captor.getValue();

        assertEquals(LeaveBalanceTransactionType.LEAVE_RESERVATION_RELEASED, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("-2.0000")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(BigDecimal.ZERO));

        assertEquals(0, transaction.getReservedAfter().compareTo(BigDecimal.ZERO));

        assertEquals(leaveRequest, transaction.getLeaveRequest());

        assertEquals(actorUserId, transaction.getActorUserId());
    }

    @Test
    void shouldApproveReservedLeaveDaysAndCreateLedgerTransaction() {

        Organisation organisation = new Organisation("Leave Approval Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.reserveDays(new BigDecimal("2.0000"));

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                employee,
                leaveType,
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave",
                UUID.randomUUID());

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        leaveBalanceService.approveLeaveDays(organisationId, accountId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("-2.0000")));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);

        ArgumentCaptor<LeaveBalanceTransaction> captor = ArgumentCaptor.forClass(LeaveBalanceTransaction.class);

        verify(leaveBalanceTransactionRepository).saveAndFlush(captor.capture());

        LeaveBalanceTransaction transaction = captor.getValue();

        assertEquals(LeaveBalanceTransactionType.LEAVE_APPROVED, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("-2.0000")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(new BigDecimal("-2.0000")));

        assertEquals(0, transaction.getReservedAfter().compareTo(BigDecimal.ZERO));

        assertEquals(leaveRequest, transaction.getLeaveRequest());

        assertEquals(actorUserId, transaction.getActorUserId());
    }

    @Test
    void shouldApplyManualAdjustmentAndCreateLedgerTransaction() {

        Organisation organisation = new Organisation("Manual Adjustment Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        leaveBalanceService.applyManualAdjustment(organisationId, accountId, new BigDecimal("2.0000"), actorUserId, "HR balance correction");

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);

        ArgumentCaptor<LeaveBalanceTransaction> captor = ArgumentCaptor.forClass(LeaveBalanceTransaction.class);

        verify(leaveBalanceTransactionRepository).saveAndFlush(captor.capture());

        LeaveBalanceTransaction transaction = captor.getValue();

        assertEquals(LeaveBalanceTransactionType.MANUAL_ADJUSTMENT, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, transaction.getReservedAfter().compareTo(BigDecimal.ZERO));

        assertNull(transaction.getLeaveRequest());

        assertEquals(actorUserId, transaction.getActorUserId());

        assertEquals("HR balance correction", transaction.getComment());
    }

    @Test
    void shouldFindLeaveAccountByEmployeeAndLeaveType() {

        Organisation organisation = new Organisation("Leave Account Lookup Service Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.of(account));

        EmployeeLeaveAccount found = leaveBalanceService.getLeaveAccount(organisationId, employeeId, leaveTypeId);

        assertSame(account, found);
    }

    @Test
    void shouldRejectLeaveAccountLookupWhenNotFound() {

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leaveBalanceService.getLeaveAccount(organisationId, employeeId, leaveTypeId));
    }

    @Test
    void shouldReserveLeaveDaysByLeaveRequest() {

        Organisation organisation = mock(Organisation.class);

        Employee employee = mock(Employee.class);

        LeaveType leaveType = mock(LeaveType.class);

        LeaveRequest leaveRequest = mock(LeaveRequest.class);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employee.getId()).thenReturn(employeeId);

        when(leaveType.getId()).thenReturn(leaveTypeId);

        when(leaveRequest.getEmployee()).thenReturn(employee);

        when(leaveRequest.getLeaveType()).thenReturn(leaveType);

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.of(account));

        when(account.getEmployee()).thenReturn(employee);

        when(account.getLeaveType()).thenReturn(leaveType);

        when(account.getOrganisation()).thenReturn(organisation);

        when(account.getCurrentBalance()).thenReturn(BigDecimal.ZERO);

        when(account.getReservedDays()).thenReturn(new BigDecimal("2.0000"));

        leaveBalanceService.reserveLeaveDays(organisationId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        verify(account).reserveDays(new BigDecimal("2.0000"));
    }

    @Test
    void shouldApproveLeaveDaysByLeaveRequest() {

        Organisation organisation = mock(Organisation.class);

        Employee employee = mock(Employee.class);

        LeaveType leaveType = mock(LeaveType.class);

        LeaveRequest leaveRequest = mock(LeaveRequest.class);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employee.getId()).thenReturn(employeeId);

        when(leaveType.getId()).thenReturn(leaveTypeId);

        when(leaveRequest.getEmployee()).thenReturn(employee);

        when(leaveRequest.getLeaveType()).thenReturn(leaveType);

        when(account.getEmployee()).thenReturn(employee);

        when(account.getLeaveType()).thenReturn(leaveType);

        when(account.getOrganisation()).thenReturn(organisation);

        when(account.getCurrentBalance()).thenReturn(new BigDecimal("-2.0000"));

        when(account.getReservedDays()).thenReturn(BigDecimal.ZERO);

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.of(account));

        leaveBalanceService.approveLeaveDays(organisationId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        verify(account).approveReservedDays(new BigDecimal("2.0000"));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);
    }

    @Test
    void shouldReleaseLeaveDaysByLeaveRequest() {

        Organisation organisation = mock(Organisation.class);

        Employee employee = mock(Employee.class);

        LeaveType leaveType = mock(LeaveType.class);

        LeaveRequest leaveRequest = mock(LeaveRequest.class);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        UUID organisationId = UUID.randomUUID();

        UUID employeeId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        UUID actorUserId = UUID.randomUUID();

        when(employee.getId()).thenReturn(employeeId);

        when(leaveType.getId()).thenReturn(leaveTypeId);

        when(leaveRequest.getEmployee()).thenReturn(employee);

        when(leaveRequest.getLeaveType()).thenReturn(leaveType);

        when(account.getEmployee()).thenReturn(employee);

        when(account.getLeaveType()).thenReturn(leaveType);

        when(account.getOrganisation()).thenReturn(organisation);

        when(account.getCurrentBalance()).thenReturn(BigDecimal.ZERO);

        when(account.getReservedDays()).thenReturn(BigDecimal.ZERO);

        when(employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(organisationId, employeeId, leaveTypeId)).thenReturn(Optional.of(account));

        leaveBalanceService.releaseLeaveDays(organisationId, leaveRequest, new BigDecimal("2.0000"), actorUserId);

        verify(account).releaseReservedDays(new BigDecimal("2.0000"));

        verify(employeeLeaveAccountRepository).saveAndFlush(account);
    }

    @Test
    void shouldReturnTransactionsForLeaveAccount() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        LeaveBalanceTransaction accrual = mock(LeaveBalanceTransaction.class);

        LeaveBalanceTransaction adjustment = mock(LeaveBalanceTransaction.class);

        when(employeeLeaveAccountRepository.findByIdAndOrganisationId(accountId, organisationId)).thenReturn(Optional.of(account));

        when(leaveBalanceTransactionRepository.findAllByOrganisationIdAndLeaveAccountIdOrderByCreatedAtAsc(organisationId, accountId)).thenReturn(List.of(accrual, adjustment));

        List<LeaveBalanceTransaction> result = leaveBalanceService.getTransactions(organisationId, accountId);

        assertEquals(2, result.size());

        assertSame(accrual, result.get(0));

        assertSame(adjustment, result.get(1));
    }
}