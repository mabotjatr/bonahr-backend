package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class LeaveBalanceTransactionRepositoryTest {

    @Autowired
    private LeaveBalanceTransactionRepository leaveBalanceTransactionRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Test
    void shouldPersistAndFindTransactionWithinOrganisation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Balance Transaction Repository " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        LeaveBalanceTransaction transaction = leaveBalanceTransactionRepository.saveAndFlush(
                new LeaveBalanceTransaction(
                        organisation,
                        account,
                        null,
                        LeaveBalanceTransactionType.ACCRUAL,
                        new BigDecimal("1.2500"),
                        new BigDecimal("1.2500"),
                        BigDecimal.ZERO,
                        UUID.randomUUID(),
                        "Monthly accrual"));

        LeaveBalanceTransaction found = leaveBalanceTransactionRepository.findByIdAndOrganisationId(transaction.getId(), organisation.getId()).orElseThrow();

        assertEquals(transaction.getId(), found.getId());

        assertEquals(LeaveBalanceTransactionType.ACCRUAL, found.getTransactionType());

        assertEquals(0, found.getAmount().compareTo(new BigDecimal("1.2500")));

        assertEquals(account.getId(), found.getLeaveAccount().getId());
    }

    @Test
    void shouldFindTransactionsForLeaveAccountInChronologicalOrder() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Balance History Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        LeaveBalanceTransaction first = leaveBalanceTransactionRepository.saveAndFlush(
                new LeaveBalanceTransaction(
                        organisation,
                        account,
                        null,
                        LeaveBalanceTransactionType.ACCRUAL,
                        new BigDecimal("1.2500"),
                        new BigDecimal("1.2500"),
                        BigDecimal.ZERO,
                        UUID.randomUUID(),
                        "First accrual"));

        LeaveBalanceTransaction second = leaveBalanceTransactionRepository.saveAndFlush(
                new LeaveBalanceTransaction(
                        organisation,
                        account,
                        null,
                        LeaveBalanceTransactionType.MANUAL_ADJUSTMENT,
                        new BigDecimal("1.0000"),
                        new BigDecimal("2.2500"),
                        BigDecimal.ZERO,
                        UUID.randomUUID(),
                        "Manual adjustment"));

        var transactions = leaveBalanceTransactionRepository.findAllByOrganisationIdAndLeaveAccountIdOrderByCreatedAtAsc(organisation.getId(), account.getId());

        assertEquals(2, transactions.size());

        assertEquals(first.getId(), transactions.get(0).getId());

        assertEquals(second.getId(), transactions.get(1).getId());
    }
}