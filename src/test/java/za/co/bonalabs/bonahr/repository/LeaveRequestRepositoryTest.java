package za.co.bonalabs.bonahr.repository;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.bonalabs.bonahr.entity.*;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LeaveRequestRepositoryTest {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnEmptyWhenLeaveRequestDoesNotExist() {

        UUID leaveRequestId = UUID.randomUUID();

        UUID organisationId = UUID.randomUUID();

        var result = leaveRequestRepository.findByIdAndOrganisationId(leaveRequestId, organisationId);

        assertTrue(result.isEmpty());
    }

    @Test
    @Transactional
    void shouldPersistAndRetrieveLeaveRequest() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Repository Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        UUID requestedByUserId = UUID.randomUUID();

        LeaveRequest leaveRequest = leaveRequestRepository.saveAndFlush(new LeaveRequest(organisation, employee, leaveType, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), "Family holiday", requestedByUserId));

        UUID leaveRequestId = leaveRequest.getId();

        entityManager.clear();

        LeaveRequest found = leaveRequestRepository.findByIdAndOrganisationId(leaveRequestId, organisation.getId()).orElseThrow();

        assertEquals(leaveRequestId, found.getId());

        assertEquals(organisation.getId(), found.getOrganisation().getId());

        assertEquals(employee.getId(), found.getEmployee().getId());

        assertEquals(leaveType.getId(), found.getLeaveType().getId());

        assertEquals(LocalDate.of(2026, 11, 2), found.getStartDate());

        assertEquals(LocalDate.of(2026, 11, 6), found.getEndDate());

        assertEquals(LeaveRequestStatus.PENDING, found.getStatus());

        assertEquals("Family holiday", found.getReason());

        assertEquals(requestedByUserId, found.getRequestedByUserId());

        assertNotNull(found.getRequestedAt());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());

        assertNull(found.getDecisionComment());
        assertNull(found.getDecidedByUserId());
        assertNull(found.getDecidedAt());
    }

    @Test
    @Transactional
    void shouldNotFindLeaveRequestFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestRepository.saveAndFlush(new LeaveRequest(
                organisationA,
                employee,
                leaveType,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3),
                "December leave",
                UUID.randomUUID()));

        entityManager.clear();

        var result = leaveRequestRepository.findByIdAndOrganisationId(leaveRequest.getId(), organisationB.getId());

        assertTrue(result.isEmpty());
    }
}