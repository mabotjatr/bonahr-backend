package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveTypeRequest;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeaveTypeRequest;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.LeaveTypeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LeaveTypeServiceTest {

    @Autowired
    private LeaveTypeService leaveTypeService;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldCreateLeaveTypeForOrganisation() {

        Organisation organisation = new Organisation("Leave Type Service Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        CreateLeaveTypeRequest request = new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true);

        LeaveType leaveType = leaveTypeService.createLeaveType(organisation.getId(), request);

        assertNotNull(leaveType.getId());

        assertEquals(organisation.getId(), leaveType.getOrganisation().getId());

        assertEquals("ANNUAL", leaveType.getCode());

        assertEquals("Annual Leave", leaveType.getName());

        assertTrue(leaveType.isPaid());
        assertTrue(leaveType.isRequiresApproval());
        assertTrue(leaveType.isActive());

        assertTrue(leaveTypeRepository.existsById(leaveType.getId()));
    }

    @Test
    void shouldRejectDuplicateLeaveTypeCodeWithinSameOrganisation() {

        Organisation organisation = new Organisation("Duplicate Leave Type Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        leaveTypeService.createLeaveType(organisation.getId(), new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () ->
                leaveTypeService.createLeaveType(finalOrganisation.getId(), new CreateLeaveTypeRequest(
                        "annual",
                        "Another Annual Leave",
                        true,
                        true)));
    }

    @Test
    void shouldAllowSameLeaveTypeCodeAcrossDifferentOrganisations() {

        Organisation organisationA = new Organisation("Leave Service Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Service Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveTypeA = leaveTypeService.createLeaveType(
                organisationA.getId(),
                new CreateLeaveTypeRequest("annual", "Annual Leave", true, true));

        LeaveType leaveTypeB = leaveTypeService.createLeaveType(
                organisationB.getId(),
                new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        assertEquals("ANNUAL", leaveTypeA.getCode());

        assertEquals("ANNUAL", leaveTypeB.getCode());

        assertNotEquals(leaveTypeA.getOrganisation().getId(), leaveTypeB.getOrganisation().getId());
    }

    @Test
    void shouldReturnLeaveTypeForOrganisation() {

        Organisation organisation = new Organisation("Leave Type Read Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType created = leaveTypeService.createLeaveType(
                organisation.getId(),
                new CreateLeaveTypeRequest("SICK", "Sick Leave", true, true));

        LeaveType retrieved = leaveTypeService.getLeaveType(organisation.getId(), created.getId());

        assertEquals(created.getId(), retrieved.getId());

        assertEquals(organisation.getId(), retrieved.getOrganisation().getId());

        assertEquals("SICK", retrieved.getCode());
    }

    @Test
    void shouldNotReturnLeaveTypeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Leave Read Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Read Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveType = leaveTypeService.createLeaveType(
                organisationA.getId(),
                new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        UUID organisationBId = organisationB.getId();

        UUID leaveTypeId = leaveType.getId();

        assertThrows(ResourceNotFoundException.class, () -> leaveTypeService.getLeaveType(organisationBId, leaveTypeId));
    }

    @Test
    void shouldReturnLeaveTypesForOrganisationOrderedByName() {

        Organisation organisationA = new Organisation("Leave List Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave List Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        leaveTypeService.createLeaveType(organisationA.getId(), new CreateLeaveTypeRequest("SICK", "Sick Leave", true, true));

        leaveTypeService.createLeaveType(organisationA.getId(), new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        leaveTypeService.createLeaveType(
                organisationB.getId(),
                new CreateLeaveTypeRequest("FAMILY", "Family Responsibility Leave", true, true));

        List<LeaveType> leaveTypes = leaveTypeService.getLeaveTypes(organisationA.getId());

        assertEquals(2, leaveTypes.size());

        assertEquals("Annual Leave", leaveTypes.get(0).getName());

        assertEquals("Sick Leave", leaveTypes.get(1).getName());

        Organisation finalOrganisationA = organisationA;
        assertTrue(leaveTypes.stream().allMatch(leaveType -> leaveType.getOrganisation().getId().equals(finalOrganisationA.getId())));
    }

    @Test
    void shouldUpdateLeaveType() {

        Organisation organisation = new Organisation("Leave Type Update Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeService.createLeaveType(
                organisation.getId(),
                new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        UpdateLeaveTypeRequest request = new UpdateLeaveTypeRequest("Annual Paid Leave", true, false);

        LeaveType updated = leaveTypeService.updateLeaveType(organisation.getId(), leaveType.getId(), request);

        assertEquals("ANNUAL", updated.getCode());

        assertEquals("Annual Paid Leave", updated.getName());

        assertTrue(updated.isPaid());

        assertFalse(updated.isRequiresApproval());

        assertTrue(updated.isActive());
    }

    @Test
    void shouldNotAllowOrganisationToUpdateAnotherOrganisationsLeaveType() {

        Organisation organisationA = new Organisation("Leave Update Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Update Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveType = leaveTypeService.createLeaveType(
                organisationA.getId(),
                new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        UpdateLeaveTypeRequest request = new UpdateLeaveTypeRequest("Changed Leave", false, false);

        UUID organisationBId = organisationB.getId();
        UUID leaveTypeId = leaveType.getId();

        assertThrows(ResourceNotFoundException.class, () -> leaveTypeService.updateLeaveType(organisationBId, leaveTypeId, request));

        LeaveType unchanged = leaveTypeRepository.findById(leaveTypeId).orElseThrow();

        assertEquals("Annual Leave", unchanged.getName());

        assertTrue(unchanged.isPaid());
        assertTrue(unchanged.isRequiresApproval());
    }

    @Test
    void shouldDeactivateLeaveType() {

        Organisation organisation = new Organisation("Leave Type Deactivate Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeService.createLeaveType(
                organisation.getId(),
                new CreateLeaveTypeRequest("ANNUAL", "Annual Leave", true, true));

        LeaveType updated = leaveTypeService.updateLeaveTypeStatus(organisation.getId(), leaveType.getId(), false);

        assertFalse(updated.isActive());

        assertEquals("ANNUAL", updated.getCode());

        assertEquals("Annual Leave", updated.getName());
    }

    @Test
    void shouldReactivateLeaveType() {

        Organisation organisation = new Organisation("Leave Type Reactivate Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeService.createLeaveType(
                organisation.getId(),
                new CreateLeaveTypeRequest("SICK", "Sick Leave", true, true));

        leaveTypeService.updateLeaveTypeStatus(organisation.getId(), leaveType.getId(), false);

        LeaveType reactivated = leaveTypeService.updateLeaveTypeStatus(organisation.getId(), leaveType.getId(), true);

        assertTrue(reactivated.isActive());

        assertEquals("SICK", reactivated.getCode());

        assertEquals("Sick Leave", reactivated.getName());
    }
}