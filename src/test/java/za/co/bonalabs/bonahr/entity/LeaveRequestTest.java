package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class LeaveRequestTest {

    @Test
    void shouldCreatePendingLeaveRequest() {

        Organisation organisation = mock(Organisation.class);

        Employee employee = mock(Employee.class);

        LeaveType leaveType = mock(LeaveType.class);

        UUID requestedByUserId = UUID.randomUUID();

        LocalDate startDate = LocalDate.of(2026, 11, 2);

        LocalDate endDate = LocalDate.of(2026, 11, 6);

        LeaveRequest leaveRequest = new LeaveRequest(organisation, employee, leaveType, startDate, endDate, "Family holiday", requestedByUserId);

        assertSame(organisation, leaveRequest.getOrganisation());

        assertSame(employee, leaveRequest.getEmployee());

        assertSame(leaveType, leaveRequest.getLeaveType());

        assertEquals(startDate, leaveRequest.getStartDate());

        assertEquals(endDate, leaveRequest.getEndDate());

        assertEquals("Family holiday", leaveRequest.getReason());

        assertEquals(requestedByUserId, leaveRequest.getRequestedByUserId());

        assertEquals(LeaveRequestStatus.PENDING, leaveRequest.getStatus());
    }

    @Test
    void shouldAllowApprovedLeaveRequestToBeCancelled() {

        Organisation organisation = new Organisation("Test Organisation");

        Employee employee = new Employee(organisation, "EMP-001", "Jane", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeaveRequest leaveRequest = new LeaveRequest(
                organisation,
                employee, leaveType,
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 13),
                "Annual leave",
                UUID.randomUUID());

        leaveRequest.approve(UUID.randomUUID(), "Approved");

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveRequestStatus.APPROVED);

        leaveRequest.cancel();

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveRequestStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancellingRejectedOrAlreadyCancelledLeaveRequest() {

        Organisation organisation = new Organisation("Test Organisation");

        Employee employee = new Employee(organisation, "EMP-001", "Jane", "Doe");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeaveRequest rejectedRequest = new LeaveRequest(
                organisation,
                employee,
                leaveType,
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 13),
                "Rejected leave",
                UUID.randomUUID());

        rejectedRequest.reject(UUID.randomUUID(), "Rejected");

        assertThatThrownBy(rejectedRequest::cancel).isInstanceOf(IllegalStateException.class).hasMessage("Only pending or approved leave requests can be cancelled");

        LeaveRequest cancelledRequest = new LeaveRequest(
                organisation,
                employee,
                leaveType,
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 20),
                "Cancelled leave",
                UUID.randomUUID());

        cancelledRequest.cancel();

        assertThatThrownBy(cancelledRequest::cancel).isInstanceOf(IllegalStateException.class).hasMessage("Only pending or approved leave requests can be cancelled");
    }
}