package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LeaveBalanceTransactionTest {

    @Test
    void shouldCreateAccrualTransaction() {

        Organisation organisation = new Organisation("Balance Transaction Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        UUID actorUserId = UUID.randomUUID();

        LeaveBalanceTransaction transaction = new LeaveBalanceTransaction(organisation, account, null, LeaveBalanceTransactionType.ACCRUAL, new BigDecimal("1.2500"), new BigDecimal("1.2500"), BigDecimal.ZERO, actorUserId, "Monthly accrual");

        assertSame(organisation, transaction.getOrganisation());

        assertSame(account, transaction.getLeaveAccount());

        assertNull(transaction.getLeaveRequest());

        assertEquals(LeaveBalanceTransactionType.ACCRUAL, transaction.getTransactionType());

        assertEquals(0, transaction.getAmount().compareTo(new BigDecimal("1.2500")));

        assertEquals(0, transaction.getBalanceAfter().compareTo(new BigDecimal("1.2500")));

        assertEquals(0, transaction.getReservedAfter().compareTo(BigDecimal.ZERO));

        assertEquals(actorUserId, transaction.getActorUserId());

        assertEquals("Monthly accrual", transaction.getComment());
    }

    @Test
    void shouldRejectNegativeReservedAfter() {

        Organisation organisation = new Organisation("Negative Reserved Transaction Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertThrows(IllegalArgumentException.class, () -> new LeaveBalanceTransaction(organisation, account, null, LeaveBalanceTransactionType.LEAVE_APPROVED, new BigDecimal("-1.0000"), new BigDecimal("-1.0000"), new BigDecimal("-1.0000"), UUID.randomUUID(), "Invalid reserved balance"));
    }
}