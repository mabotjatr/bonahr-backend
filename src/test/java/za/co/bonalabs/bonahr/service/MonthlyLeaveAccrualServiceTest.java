package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyLeaveAccrualServiceTest {

    @Mock
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @InjectMocks
    private MonthlyLeaveAccrualService monthlyLeaveAccrualService;

    @Test
    void shouldApplyMonthlyAccrualToEligibleAccount() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        LocalDate accrualDate = LocalDate.of(2026, 10, 1);

        Organisation organisation = mock(Organisation.class);

        LeavePolicy leavePolicy = mock(LeavePolicy.class);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        when(employeeLeaveAccountRepository.findAllByLeavePolicyAccrualMethodAndLeavePolicyActiveTrueAndEmployeeStatus(LeaveAccrualMethod.MONTHLY, EmployeeStatus.ACTIVE)).thenReturn(List.of(account));

        when(account.getOrganisation()).thenReturn(organisation);

        when(organisation.getId()).thenReturn(organisationId);

        when(account.getId()).thenReturn(accountId);

        when(account.getLeavePolicy()).thenReturn(leavePolicy);

        when(leavePolicy.getAccrualRate()).thenReturn(new BigDecimal("1.2500"));

        when(account.getLastAccrualDate()).thenReturn(null);

        monthlyLeaveAccrualService.processMonthlyAccruals(accrualDate);

        verify(leaveBalanceService).applyAccrual(organisationId, accountId, new BigDecimal("1.2500"), accrualDate, null);
    }

    @Test
    void shouldSkipAccountAlreadyAccruedForSameMonth() {

        LocalDate accrualDate = LocalDate.of(2026, 10, 1);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        when(employeeLeaveAccountRepository.findAllByLeavePolicyAccrualMethodAndLeavePolicyActiveTrueAndEmployeeStatus(LeaveAccrualMethod.MONTHLY, EmployeeStatus.ACTIVE)).thenReturn(List.of(account));

        when(account.getLastAccrualDate()).thenReturn(LocalDate.of(2026, 10, 5));

        monthlyLeaveAccrualService.processMonthlyAccruals(accrualDate);

        verifyNoInteractions(leaveBalanceService);
    }

    @Test
    void shouldAccrueAgainWhenLastAccrualWasInPreviousMonth() {

        UUID organisationId = UUID.randomUUID();

        UUID accountId = UUID.randomUUID();

        LocalDate accrualDate = LocalDate.of(2026, 10, 1);

        Organisation organisation = mock(Organisation.class);

        LeavePolicy leavePolicy = mock(LeavePolicy.class);

        EmployeeLeaveAccount account = mock(EmployeeLeaveAccount.class);

        when(employeeLeaveAccountRepository.findAllByLeavePolicyAccrualMethodAndLeavePolicyActiveTrueAndEmployeeStatus(LeaveAccrualMethod.MONTHLY, EmployeeStatus.ACTIVE)).thenReturn(List.of(account));

        when(account.getLastAccrualDate()).thenReturn(LocalDate.of(2026, 9, 1));

        when(account.getOrganisation()).thenReturn(organisation);

        when(organisation.getId()).thenReturn(organisationId);

        when(account.getId()).thenReturn(accountId);

        when(account.getLeavePolicy()).thenReturn(leavePolicy);

        when(leavePolicy.getAccrualRate()).thenReturn(new BigDecimal("1.2500"));

        monthlyLeaveAccrualService.processMonthlyAccruals(accrualDate);

        verify(leaveBalanceService).applyAccrual(organisationId, accountId, new BigDecimal("1.2500"), accrualDate, null);
    }

    @Test
    void shouldContinueProcessingWhenOneAccountAccrualFails() {

        LocalDate accrualDate = LocalDate.of(2026, 10, 1);

        UUID organisationIdOne = UUID.randomUUID();

        UUID organisationIdTwo = UUID.randomUUID();

        UUID accountIdOne = UUID.randomUUID();

        UUID accountIdTwo = UUID.randomUUID();

        Organisation organisationOne = mock(Organisation.class);

        Organisation organisationTwo = mock(Organisation.class);

        LeavePolicy policyOne = mock(LeavePolicy.class);

        LeavePolicy policyTwo = mock(LeavePolicy.class);

        EmployeeLeaveAccount accountOne = mock(EmployeeLeaveAccount.class);

        EmployeeLeaveAccount accountTwo = mock(EmployeeLeaveAccount.class);

        when(employeeLeaveAccountRepository.findAllByLeavePolicyAccrualMethodAndLeavePolicyActiveTrueAndEmployeeStatus(LeaveAccrualMethod.MONTHLY, EmployeeStatus.ACTIVE)).thenReturn(List.of(accountOne, accountTwo));

        when(accountOne.getLastAccrualDate()).thenReturn(null);

        when(accountOne.getOrganisation()).thenReturn(organisationOne);

        when(organisationOne.getId()).thenReturn(organisationIdOne);

        when(accountOne.getId()).thenReturn(accountIdOne);

        when(accountOne.getLeavePolicy()).thenReturn(policyOne);

        when(policyOne.getAccrualRate()).thenReturn(new BigDecimal("1.2500"));

        when(accountTwo.getLastAccrualDate()).thenReturn(null);

        when(accountTwo.getOrganisation()).thenReturn(organisationTwo);

        when(organisationTwo.getId()).thenReturn(organisationIdTwo);

        when(accountTwo.getId()).thenReturn(accountIdTwo);

        when(accountTwo.getLeavePolicy()).thenReturn(policyTwo);

        when(policyTwo.getAccrualRate()).thenReturn(new BigDecimal("1.5000"));

        doThrow(new IllegalStateException("Accrual failed")).when(leaveBalanceService).applyAccrual(organisationIdOne, accountIdOne, new BigDecimal("1.2500"), accrualDate, null);

        assertDoesNotThrow(() -> monthlyLeaveAccrualService.processMonthlyAccruals(accrualDate));

        verify(leaveBalanceService).applyAccrual(organisationIdTwo, accountIdTwo, new BigDecimal("1.5000"), accrualDate, null);
    }
}