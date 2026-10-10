package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
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
import java.util.Locale;
import java.util.UUID;

@Service
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final OrganisationRepository organisationRepository;

    public LeaveTypeService(LeaveTypeRepository leaveTypeRepository, OrganisationRepository organisationRepository) {
        this.leaveTypeRepository = leaveTypeRepository;
        this.organisationRepository = organisationRepository;
    }

    @Transactional
    public LeaveType createLeaveType(UUID organisationId, CreateLeaveTypeRequest request) {

        Organisation organisation = organisationRepository.findById(organisationId).orElseThrow(() ->
                new ResourceNotFoundException("Organisation not found: " + organisationId));

        String normalizedCode = request.code().trim().toUpperCase(Locale.ROOT);

        if (leaveTypeRepository.existsByOrganisationIdAndCodeIgnoreCase(organisationId, normalizedCode)) {

            throw new DuplicateResourceException("Leave type code already exists: " + normalizedCode);
        }

        LeaveType leaveType = new LeaveType(organisation, normalizedCode, request.name(), request.paid(), request.requiresApproval());

        return leaveTypeRepository.saveAndFlush(leaveType);
    }

    @Transactional(readOnly = true)
    public LeaveType getLeaveType(UUID organisationId, UUID leaveTypeId) {

        return leaveTypeRepository.findByIdAndOrganisationId(leaveTypeId, organisationId).orElseThrow(() ->
                new ResourceNotFoundException("Leave type not found: " + leaveTypeId));
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getLeaveTypes(UUID organisationId) {

        return leaveTypeRepository.findAllByOrganisationIdOrderByNameAsc(organisationId);
    }

    @Transactional
    public LeaveType updateLeaveType(UUID organisationId, UUID leaveTypeId, UpdateLeaveTypeRequest request) {

        LeaveType leaveType = getLeaveType(organisationId, leaveTypeId);

        leaveType.setName(request.name());

        leaveType.setPaid(request.paid());

        leaveType.setRequiresApproval(request.requiresApproval());

        return leaveTypeRepository.saveAndFlush(leaveType);
    }

    @Transactional
    public LeaveType updateLeaveTypeStatus(UUID organisationId, UUID leaveTypeId, boolean active) {

        LeaveType leaveType = getLeaveType(organisationId, leaveTypeId);

        leaveType.setActive(active);

        return leaveTypeRepository.saveAndFlush(leaveType);
    }
}