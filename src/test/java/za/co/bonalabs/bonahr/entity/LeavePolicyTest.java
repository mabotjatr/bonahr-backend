package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LeavePolicyTest {

    @Test
    void shouldCreateMonthlyAccrualPolicy() {

        Organisation organisation = new Organisation("Leave Policy Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy policy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        assertSame(organisation, policy.getOrganisation());

        assertSame(leaveType, policy.getLeaveType());

        assertEquals(LeaveAccrualMethod.MONTHLY, policy.getAccrualMethod());

        assertEquals(new BigDecimal("1.2500"), policy.getAccrualRate());

        assertTrue(policy.isAllowNegative());

        assertEquals(new BigDecimal("3.0000"), policy.getNegativeLimitDays());

        assertTrue(policy.isActive());
    }

    @Test
    void shouldRejectNegativeLimitWhenNegativeLeaveIsDisabled() {

        Organisation organisation = new Organisation("Leave Policy Validation Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        assertThrows(IllegalArgumentException.class, () -> new LeavePolicy(
                        organisation,
                        leaveType,
                        LeaveAccrualMethod.MONTHLY,
                        new BigDecimal("1.2500"),
                        false,
                        new BigDecimal("3.0000")));
    }

    @Test
    void shouldRejectZeroAccrualRate() {

        Organisation organisation = new Organisation("Leave Policy Accrual Validation Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        assertThrows(IllegalArgumentException.class, () -> new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                BigDecimal.ZERO,
                false,
                BigDecimal.ZERO));
    }

    @Test
    void shouldRejectNegativeNegativeLimit() {

        Organisation organisation = new Organisation("Leave Policy Negative Limit Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        assertThrows(IllegalArgumentException.class, () -> new LeavePolicy(
                organisation,
                leaveType,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"),
                true,
                new BigDecimal("-1.0000")));
    }
}