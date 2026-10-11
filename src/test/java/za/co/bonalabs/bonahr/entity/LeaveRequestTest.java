package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

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
}