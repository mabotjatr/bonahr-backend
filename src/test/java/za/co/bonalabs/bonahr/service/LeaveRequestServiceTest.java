package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveRequestRequest;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.LeaveTypeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class LeaveRequestServiceTest {

    @MockitoBean
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private LeaveRequestService leaveRequestService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldCreatePendingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Service Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        UUID requestedByUserId = UUID.randomUUID();

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                "Family holiday");

        LeaveRequest result = leaveRequestService.createLeaveRequest(organisation.getId(), requestedByUserId, request);

        assertEquals(organisation.getId(), result.getOrganisation().getId());

        assertEquals(employee.getId(), result.getEmployee().getId());

        assertEquals(leaveType.getId(), result.getLeaveType().getId());

        assertEquals(LeaveRequestStatus.PENDING, result.getStatus());

        assertEquals(LocalDate.of(2026, 11, 2), result.getStartDate());

        assertEquals(LocalDate.of(2026, 11, 6), result.getEndDate());

        assertEquals("Family holiday", result.getReason());

        assertEquals(requestedByUserId, result.getRequestedByUserId());
    }

    @Test
    void shouldRejectEmployeeFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant B " + UUID.randomUUID()));

        Employee employeeFromOrganisationB = employeeRepository.saveAndFlush(
                new Employee(organisationB, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employeeFromOrganisationB.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                "Family holiday");

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.createLeaveRequest(organisationA.getId(), UUID.randomUUID(), request));
    }

    @Test
    void shouldRejectLeaveTypeFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveTypeFromOrganisationB = leaveTypeRepository.saveAndFlush(
                new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveTypeFromOrganisationB.getId(),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                "Family holiday");

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.createLeaveRequest(organisationA.getId(), UUID.randomUUID(), request));
    }

    @Test
    void shouldRejectInactiveLeaveType() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Inactive Leave Type Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        leaveType.setActive(false);
        leaveTypeRepository.saveAndFlush(leaveType);

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 6),
                "Family holiday");

        assertThrows(IllegalStateException.class, () -> leaveRequestService.createLeaveRequest(organisation.getId(), UUID.randomUUID(), request));
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invalid Leave Dates Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 6),
                LocalDate.of(2026, 11, 2),
                "Invalid date range");

        assertThrows(IllegalArgumentException.class, () -> leaveRequestService.createLeaveRequest(organisation.getId(), UUID.randomUUID(), request));
    }

    @Test
    void shouldGetLeaveRequestForOrganisation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Get Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest created = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 11, 6),
                        "Family holiday"));

        LeaveRequest result = leaveRequestService.getLeaveRequest(organisation.getId(), created.getId());

        assertEquals(created.getId(), result.getId());

        assertEquals(organisation.getId(), result.getOrganisation().getId());
    }

    @Test
    void shouldNotGetLeaveRequestFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request Get Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request Get Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest created = leaveRequestService.createLeaveRequest(
                organisationA.getId(), UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 11, 6),
                        "Family holiday"));

        assertThrows(ResourceNotFoundException.class, () -> leaveRequestService.getLeaveRequest(organisationB.getId(), created.getId()));
    }

    @Test
    void shouldListLeaveRequestsForOrganisationOrderedByStartDate() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request List A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request List B " + UUID.randomUUID()));

        Employee employeeA = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        Employee employeeB = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        LeaveType leaveTypeA = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest laterRequest = leaveRequestService.createLeaveRequest(
                organisationA.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employeeA.getId(),
                        leaveTypeA.getId(),
                        LocalDate.of(2026, 12, 10),
                        LocalDate.of(2026, 12, 12),
                        "Later leave"));

        LeaveRequest earlierRequest = leaveRequestService.createLeaveRequest(
                organisationA.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employeeA.getId(),
                        leaveTypeA.getId(),
                        LocalDate.of(2026, 11, 3),
                        LocalDate.of(2026, 11, 5),
                        "Earlier leave"));

        leaveRequestService.createLeaveRequest(
                organisationB.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employeeB.getId(),
                        leaveTypeB.getId(),
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                        "Other tenant leave"));

        List<LeaveRequest> result = leaveRequestService.getLeaveRequests(organisationA.getId());

        assertEquals(2, result.size());

        assertEquals(earlierRequest.getId(), result.get(0).getId());

        assertEquals(laterRequest.getId(), result.get(1).getId());
    }

    @Test
    void shouldApprovePendingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Approval Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        UUID decidedByUserId = UUID.randomUUID();

        LeaveRequest approved = leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), decidedByUserId, "Approved");

        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());

        assertEquals(decidedByUserId, approved.getDecidedByUserId());

        assertEquals("Approved", approved.getDecisionComment());

        assertNotNull(approved.getDecidedAt());
    }

    @Test
    void shouldRejectApprovingAlreadyApprovedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Approval Validation Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Approved");

        assertThrows(IllegalStateException.class, () ->
                leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Approve again"));
    }

    @Test
    void shouldRejectPendingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Rejection Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        UUID decidedByUserId = UUID.randomUUID();

        LeaveRequest rejected = leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), decidedByUserId, "Insufficient staffing");

        assertEquals(LeaveRequestStatus.REJECTED, rejected.getStatus());

        assertEquals(decidedByUserId, rejected.getDecidedByUserId());

        assertEquals("Insufficient staffing", rejected.getDecisionComment());

        assertNotNull(rejected.getDecidedAt());
    }


    @Test
    void shouldRejectRejectingAlreadyRejectedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Rejection Validation Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Rejected");

        assertThrows(IllegalStateException.class, () ->
                leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Reject again"));
    }

    @Test
    void shouldRejectRejectingApprovedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Cross Transition Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Approved");

        assertThrows(IllegalStateException.class, () ->
                leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Trying to reject approved leave"));
    }

    @Test
    void shouldRejectApprovingRejectedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Cross Transition Reverse Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(), UUID.randomUUID(),
                new CreateLeaveRequestRequest(employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Rejected");

        assertThrows(IllegalStateException.class, () ->
                leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Trying to approve rejected leave"));
    }

    @Test
    void shouldCancelPendingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Cancellation Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        UUID cancelledByUserId = UUID.randomUUID();

        LeaveRequest cancelled = leaveRequestService.cancelLeaveRequest(organisation.getId(), leaveRequest.getId(), cancelledByUserId);

        assertEquals(LeaveRequestStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void shouldRejectCancellingApprovedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Approved Leave Cancellation Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Approved");

        UUID cancelledByUserId = UUID.randomUUID();

        assertThrows(IllegalStateException.class, () -> leaveRequestService.cancelLeaveRequest(organisation.getId(), leaveRequest.getId(), cancelledByUserId));
    }

    @Test
    void shouldRejectCancellingRejectedLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Rejected Leave Cancellation Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), UUID.randomUUID(), "Rejected");

        UUID cancelledByUserId = UUID.randomUUID();

        assertThrows(IllegalStateException.class, () -> leaveRequestService.cancelLeaveRequest(organisation.getId(), leaveRequest.getId(), cancelledByUserId));
    }

    @Test
    void shouldReserveLeaveBalanceWhenCreatingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Balance Integration Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL-" + UUID.randomUUID(), "Annual Leave", true, true));

        UUID requestedByUserId = UUID.randomUUID();

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(employee.getId(), leaveType.getId(), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11), "Annual leave");

        LeaveRequest result = leaveRequestService.createLeaveRequest(organisation.getId(), requestedByUserId, request);

        verify(leaveBalanceService).reserveLeaveDays(organisation.getId(), result, new BigDecimal("2"), requestedByUserId);
    }

    @Test
    void shouldDeductReservedBalanceWhenApprovingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Approval Balance Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(
                new LeaveType(
                        organisation,
                        "ANNUAL-" + UUID.randomUUID(),
                        "Annual Leave",
                        true,
                        true));

        UUID requestedByUserId = UUID.randomUUID();

        UUID decidedByUserId = UUID.randomUUID();

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave");

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(organisation.getId(), requestedByUserId, request);

        leaveRequestService.approveLeaveRequest(organisation.getId(), leaveRequest.getId(), decidedByUserId, "Approved");

        verify(leaveBalanceService).approveLeaveDays(organisation.getId(), leaveRequest, new BigDecimal("2"), decidedByUserId);
    }

    @Test
    void shouldReleaseReservedBalanceWhenRejectingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Rejection Balance Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL-" + UUID.randomUUID(), "Annual Leave", true, true));

        UUID requestedByUserId = UUID.randomUUID();

        UUID decidedByUserId = UUID.randomUUID();

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave");

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(organisation.getId(), requestedByUserId, request);

        leaveRequestService.rejectLeaveRequest(organisation.getId(), leaveRequest.getId(), decidedByUserId, "Not approved");

        verify(leaveBalanceService).releaseLeaveDays(organisation.getId(), leaveRequest, new BigDecimal("2"), decidedByUserId);
    }

    @Test
    void shouldReleaseReservedBalanceWhenCancellingLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Cancellation Balance Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL-" + UUID.randomUUID(), "Annual Leave", true, true));

        UUID requestedByUserId = UUID.randomUUID();

        UUID cancelledByUserId = UUID.randomUUID();

        CreateLeaveRequestRequest request = new CreateLeaveRequestRequest(
                employee.getId(),
                leaveType.getId(),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                "Annual leave");

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(organisation.getId(), requestedByUserId, request);

        leaveRequestService.cancelLeaveRequest(organisation.getId(), leaveRequest.getId(), cancelledByUserId);

        verify(leaveBalanceService).releaseLeaveDays(organisation.getId(), leaveRequest, new BigDecimal("2"), cancelledByUserId);
    }
}