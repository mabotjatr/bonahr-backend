package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LeaveTypeRepositoryTest {

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldPersistLeaveType() {

        Organisation organisation = new Organisation("Leave Type Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        leaveType = leaveTypeRepository.saveAndFlush(leaveType);

        List<LeaveType> leaveTypes = leaveTypeRepository.findAllByOrganisationIdOrderByNameAsc(organisation.getId());

        assertEquals(1, leaveTypes.size());

        LeaveType persisted = leaveTypes.getFirst();

        assertNotNull(persisted.getId());

        assertEquals(organisation.getId(), persisted.getOrganisation().getId());

        assertEquals("ANNUAL", persisted.getCode());

        assertEquals("Annual Leave", persisted.getName());

        assertTrue(persisted.isPaid());
        assertTrue(persisted.isRequiresApproval());
        assertTrue(persisted.isActive());

        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void shouldNotReturnLeaveTypeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Leave Type Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Type Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveType = new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true);

        leaveType = leaveTypeRepository.saveAndFlush(leaveType);

        assertTrue(leaveTypeRepository.findByIdAndOrganisationId(leaveType.getId(), organisationB.getId()).isEmpty());
    }

    @Test
    void shouldAllowSameLeaveTypeCodeAcrossDifferentOrganisations() {

        Organisation organisationA = new Organisation("Leave Code Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Code Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveTypeA = new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true);

        LeaveType leaveTypeB = new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true);

        leaveTypeRepository.saveAndFlush(leaveTypeA);
        leaveTypeRepository.saveAndFlush(leaveTypeB);

        assertTrue(leaveTypeRepository.existsByOrganisationIdAndCodeIgnoreCase(organisationA.getId(), "annual"));

        assertTrue(leaveTypeRepository.existsByOrganisationIdAndCodeIgnoreCase(organisationB.getId(), "ANNUAL"));
    }
}