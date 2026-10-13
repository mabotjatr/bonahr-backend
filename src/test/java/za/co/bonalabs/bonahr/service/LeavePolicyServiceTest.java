package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.bonalabs.bonahr.dto.leave.CreateLeavePolicyRequest;
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.LeavePolicyRepository;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeavePolicyRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceTest {

    @Mock
    private LeavePolicyRepository leavePolicyRepository;

    @Mock
    private LeaveTypeService leaveTypeService;

    @InjectMocks
    private LeavePolicyService leavePolicyService;

    @Test
    void shouldCreateLeavePolicy() {

        UUID organisationId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        Organisation organisation = new Organisation("Leave Policy Service Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        CreateLeavePolicyRequest request = new CreateLeavePolicyRequest(leaveTypeId, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000"));

        when(leaveTypeService.getLeaveType(organisationId, leaveTypeId)).thenReturn(leaveType);

        when(leavePolicyRepository.saveAndFlush(any(LeavePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeavePolicy result = leavePolicyService.createLeavePolicy(organisationId, request);

        assertSame(organisation, result.getOrganisation());

        assertSame(leaveType, result.getLeaveType());

        assertEquals(LeaveAccrualMethod.MONTHLY, result.getAccrualMethod());

        assertEquals(0, result.getAccrualRate().compareTo(new BigDecimal("1.2500")));

        assertTrue(result.isAllowNegative());

        assertEquals(0, result.getNegativeLimitDays().compareTo(new BigDecimal("3.0000")));

        assertTrue(result.isActive());
    }

    @Test
    void shouldRejectDuplicatePolicyForSameLeaveType() {

        UUID organisationId = UUID.randomUUID();

        UUID leaveTypeId = UUID.randomUUID();

        Organisation organisation = new Organisation("Duplicate Policy Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy existingPolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        CreateLeavePolicyRequest request = new CreateLeavePolicyRequest(leaveTypeId, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        when(leaveTypeService.getLeaveType(organisationId, leaveTypeId)).thenReturn(leaveType);

        when(leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisationId, leaveTypeId)).thenReturn(java.util.Optional.of(existingPolicy));

        assertThrows(IllegalStateException.class, () -> leavePolicyService.createLeavePolicy(organisationId, request));
    }

    @Test
    void shouldGetLeavePolicyById() {

        UUID organisationId = UUID.randomUUID();

        UUID policyId = UUID.randomUUID();

        Organisation organisation = new Organisation("Get Policy Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        when(leavePolicyRepository.findByIdAndOrganisationId(policyId, organisationId)).thenReturn(java.util.Optional.of(leavePolicy));

        LeavePolicy result = leavePolicyService.getLeavePolicy(organisationId, policyId);

        assertSame(leavePolicy, result);
    }

    @Test
    void shouldThrowWhenLeavePolicyNotFoundForOrganisation() {

        UUID organisationId = UUID.randomUUID();

        UUID policyId = UUID.randomUUID();

        when(leavePolicyRepository.findByIdAndOrganisationId(policyId, organisationId)).thenReturn(java.util.Optional.empty());

        assertThrows(za.co.bonalabs.bonahr.exception.ResourceNotFoundException.class, () -> leavePolicyService.getLeavePolicy(organisationId, policyId));
    }

    @Test
    void shouldListLeavePoliciesForOrganisation() {

        UUID organisationId = UUID.randomUUID();

        Organisation organisation = new Organisation("List Policies Company");

        LeaveType annualLeave = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeaveType sickLeave = new LeaveType(organisation, "SICK", "Sick Leave", true, true);

        LeavePolicy annualPolicy = new LeavePolicy(organisation, annualLeave, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        LeavePolicy sickPolicy = new LeavePolicy(organisation, sickLeave, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.0000"), false, BigDecimal.ZERO);

        when(leavePolicyRepository.findAllByOrganisationId(organisationId)).thenReturn(java.util.List.of(annualPolicy, sickPolicy));

        java.util.List<LeavePolicy> result = leavePolicyService.getLeavePolicies(organisationId);

        assertEquals(2, result.size());

        assertSame(annualPolicy, result.get(0));

        assertSame(sickPolicy, result.get(1));
    }

    @Test
    void shouldUpdateLeavePolicy() {

        UUID organisationId = UUID.randomUUID();

        UUID policyId = UUID.randomUUID();

        Organisation organisation = new Organisation("Update Policy Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        UpdateLeavePolicyRequest request = new UpdateLeavePolicyRequest(LeaveAccrualMethod.UPFRONT, new BigDecimal("15.0000"), true, new BigDecimal("3.0000"));

        when(leavePolicyRepository.findByIdAndOrganisationId(policyId, organisationId)).thenReturn(java.util.Optional.of(leavePolicy));

        when(leavePolicyRepository.saveAndFlush(any(LeavePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeavePolicy result = leavePolicyService.updateLeavePolicy(organisationId, policyId, request);

        assertEquals(LeaveAccrualMethod.UPFRONT, result.getAccrualMethod());

        assertEquals(0, result.getAccrualRate().compareTo(new BigDecimal("15.0000")));

        assertTrue(result.isAllowNegative());

        assertEquals(0, result.getNegativeLimitDays().compareTo(new BigDecimal("3.0000")));
    }

    @Test
    void shouldDeactivateLeavePolicy() {

        UUID organisationId = UUID.randomUUID();

        UUID policyId = UUID.randomUUID();

        Organisation organisation = new Organisation("Policy Status Company");

        LeaveType leaveType = new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true);

        LeavePolicy leavePolicy = new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO);

        assertTrue(leavePolicy.isActive());

        when(leavePolicyRepository.findByIdAndOrganisationId(policyId, organisationId)).thenReturn(java.util.Optional.of(leavePolicy));

        when(leavePolicyRepository.saveAndFlush(any(LeavePolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeavePolicy result = leavePolicyService.updateLeavePolicyStatus(organisationId, policyId, false);

        assertFalse(result.isActive());
    }
}