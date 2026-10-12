package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeLeaveAccountTest {

    @Test
    void shouldCreateEmployeeLeaveAccountWithZeroBalances() {

        Organisation organisation = new Organisation("Employee Leave Account Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertSame(organisation, account.getOrganisation());

        assertSame(employee, account.getEmployee());

        assertSame(leaveType, account.getLeaveType());

        assertSame(leavePolicy, account.getLeavePolicy());

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertNull(account.getLastAccrualDate());
    }

    @Test
    void shouldReserveLeaveDays() {

        Organisation organisation = new Organisation("Leave Reservation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"),
                true,
                new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.reserveDays(new BigDecimal("2.0000"));

        assertEquals(0, account.getReservedDays().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectZeroDayReservation() {

        Organisation organisation = new Organisation("Invalid Reservation Company");

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

        assertThrows(IllegalArgumentException.class, () -> account.reserveDays(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectReservationBeyondNegativeLimit() {

        Organisation organisation = new Organisation("Negative Limit Reservation Company");

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

        assertThrows(IllegalStateException.class, () -> account.reserveDays(new BigDecimal("4.0000")));
    }

    @Test
    void shouldAllowReservationExactlyAtNegativeLimit() {

        Organisation organisation = new Organisation("Negative Limit Boundary Company");

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

        account.reserveDays(new BigDecimal("3.0000"));

        assertEquals(0, account.getReservedDays().compareTo(new BigDecimal("3.0000")));
    }

    @Test
    void shouldRejectReservationWhenNegativeLeaveIsDisabled() {

        Organisation organisation = new Organisation("No Negative Leave Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                false,
                BigDecimal.ZERO);

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertThrows(IllegalStateException.class, () -> account.reserveDays(new BigDecimal("1.0000")));
    }

    @Test
    void shouldReleaseReservedDays() {

        Organisation organisation = new Organisation("Release Reservation Company");

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

        account.releaseReservedDays(new BigDecimal("2.0000"));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectReleasingMoreDaysThanReserved() {

        Organisation organisation = new Organisation("Release Validation Company");

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

        account.reserveDays(new BigDecimal("1.0000"));

        assertThrows(IllegalStateException.class, () -> account.releaseReservedDays(new BigDecimal("2.0000")));
    }

    @Test
    void shouldRejectZeroDayRelease() {

        Organisation organisation = new Organisation("Release Input Validation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertThrows(IllegalArgumentException.class, () -> account.releaseReservedDays(BigDecimal.ZERO));
    }

    @Test
    void shouldApproveReservedLeaveDays() {

        Organisation organisation = new Organisation("Leave Approval Company");

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

        account.approveReservedDays(new BigDecimal("2.0000"));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("-2.0000")));
    }

    @Test
    void shouldRejectApprovingMoreDaysThanReserved() {

        Organisation organisation = new Organisation("Approval Validation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.reserveDays(new BigDecimal("1.0000"));

        assertThrows(IllegalStateException.class, () -> account.approveReservedDays(new BigDecimal("2.0000")));
    }

    @Test
    void shouldApplyLeaveAccrual() {

        Organisation organisation = new Organisation("Leave Accrual Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        LocalDate accrualDate = LocalDate.of(2026, 10, 31);

        account.applyAccrual(new BigDecimal("1.2500"), accrualDate);

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("1.2500")));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));

        assertEquals(accrualDate, account.getLastAccrualDate());
    }

    @Test
    void shouldRejectZeroAccrualAmount() {

        Organisation organisation = new Organisation("Accrual Validation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertThrows(IllegalArgumentException.class, () -> account.applyAccrual(BigDecimal.ZERO, LocalDate.of(2026, 10, 31)));
    }

    @Test
    void shouldRejectNullAccrualDate() {

        Organisation organisation = new Organisation("Accrual Date Validation Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        assertThrows(IllegalArgumentException.class, () -> account.applyAccrual(new BigDecimal("1.2500"), null));
    }

    @Test
    void shouldRejectDuplicateAccrualDate() {

        Organisation organisation = new Organisation("Duplicate Accrual Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        LocalDate accrualDate = LocalDate.of(2026, 10, 31);

        account.applyAccrual(new BigDecimal("1.2500"), accrualDate);

        assertThrows(IllegalStateException.class, () -> account.applyAccrual(new BigDecimal("1.2500"), accrualDate));
    }

    @Test
    void shouldRejectAccrualDateBeforeLastAccrualDate() {

        Organisation organisation = new Organisation("Accrual Order Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.applyAccrual(new BigDecimal("1.2500"), LocalDate.of(2026, 10, 31));

        assertThrows(IllegalStateException.class, () -> account.applyAccrual(new BigDecimal("1.2500"), LocalDate.of(2026, 9, 30)));
    }

    @Test
    void shouldApplyPositiveManualAdjustment() {

        Organisation organisation = new Organisation("Manual Adjustment Company");

        Employee employee = new Employee(organisation, "EMP001", "John", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy);

        account.applyManualAdjustment(new BigDecimal("2.0000"));

        assertEquals(0, account.getCurrentBalance().compareTo(new BigDecimal("2.0000")));

        assertEquals(0, account.getReservedDays().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectManualAdjustmentBeyondNegativeLimit() {

        Organisation organisation = new Organisation("Manual Adjustment Limit Company");

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

        assertThrows(IllegalStateException.class, () -> account.applyManualAdjustment(new BigDecimal("-4.0000")));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldRejectManualAdjustmentWhenReservedDaysWouldExceedNegativeLimit() {

        Organisation organisation = new Organisation("Manual Adjustment Reserved Balance Company");

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

        assertThrows(IllegalStateException.class, () -> account.applyManualAdjustment(new BigDecimal("-2.0000")));

        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.ZERO));

        assertEquals(0, account.getReservedDays().compareTo(new BigDecimal("2.0000")));
    }
}