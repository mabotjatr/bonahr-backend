package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateLeavePolicyRequest;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeavePolicyRequest;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.repository.LeavePolicyRepository;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class LeavePolicyService {

    private final LeavePolicyRepository leavePolicyRepository;
    private final LeaveTypeService leaveTypeService;

    public LeavePolicyService(LeavePolicyRepository leavePolicyRepository, LeaveTypeService leaveTypeService) {
        this.leavePolicyRepository = leavePolicyRepository;
        this.leaveTypeService = leaveTypeService;
    }

    @Transactional
    public LeavePolicy createLeavePolicy(UUID organisationId, CreateLeavePolicyRequest request) {

        LeaveType leaveType = leaveTypeService.getLeaveType(organisationId, request.leaveTypeId());

        boolean policyExists = leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(organisationId, request.leaveTypeId()).isPresent();

        if (policyExists) {
            throw new IllegalStateException("Leave policy already exists for leave type: " + request.leaveTypeId());
        }

        LeavePolicy leavePolicy = new LeavePolicy(leaveType.getOrganisation(), leaveType, request.accrualMethod(), request.accrualRate(), request.allowNegative(), request.negativeLimitDays());

        return leavePolicyRepository.saveAndFlush(leavePolicy);
    }

    @Transactional(readOnly = true)
    public LeavePolicy getLeavePolicy(UUID organisationId, UUID policyId) {
        return leavePolicyRepository.findByIdAndOrganisationId(
                policyId,
                organisationId).orElseThrow(() -> new ResourceNotFoundException("Leave policy not found: " + policyId));
    }

    @Transactional(readOnly = true)
    public List<LeavePolicy> getLeavePolicies(UUID organisationId) {
        return leavePolicyRepository.findAllByOrganisationId(organisationId);
    }

    @Transactional
    public LeavePolicy updateLeavePolicy(UUID organisationId, UUID policyId, UpdateLeavePolicyRequest request) {

        LeavePolicy leavePolicy = getLeavePolicy(organisationId, policyId);

        leavePolicy.update(request.accrualMethod(), request.accrualRate(), request.allowNegative(), request.negativeLimitDays());

        return leavePolicyRepository.saveAndFlush(leavePolicy);
    }

    @Transactional
    public LeavePolicy updateLeavePolicyStatus(UUID organisationId, UUID policyId, boolean active) {

        LeavePolicy leavePolicy = getLeavePolicy(organisationId, policyId);

        leavePolicy.updateStatus(active);

        return leavePolicyRepository.saveAndFlush(leavePolicy);
    }
}