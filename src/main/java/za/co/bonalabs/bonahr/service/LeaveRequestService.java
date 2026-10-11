package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveRequestRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.LeaveRequest;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.LeaveRequestRepository;

import java.util.List;
import java.util.UUID;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeService employeeService;
    private final LeaveTypeService leaveTypeService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeService employeeService, LeaveTypeService leaveTypeService) {
        this.leaveRequestRepository = leaveRequestRepository;

        this.employeeService = employeeService;

        this.leaveTypeService = leaveTypeService;
    }

    @Transactional
    public LeaveRequest createLeaveRequest(UUID organisationId, UUID requestedByUserId, CreateLeaveRequestRequest request) {

        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Employee employee = employeeService.getEmployee(organisationId, request.employeeId());

        LeaveType leaveType = leaveTypeService.getLeaveType(organisationId, request.leaveTypeId());

        if (!leaveType.isActive()) {
            throw new IllegalStateException("Leave type is inactive: " + leaveType.getCode());
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                employee.getOrganisation(),
                employee,
                leaveType,
                request.startDate(),
                request.endDate(),
                request.reason(),
                requestedByUserId);

        return leaveRequestRepository.saveAndFlush(leaveRequest);
    }

    @Transactional(readOnly = true)
    public LeaveRequest getLeaveRequest(UUID organisationId, UUID leaveRequestId) {
        return leaveRequestRepository.findByIdAndOrganisationId(leaveRequestId, organisationId).orElseThrow(() ->
                new ResourceNotFoundException("Leave request not found: " + leaveRequestId));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequests(UUID organisationId) {
        return leaveRequestRepository.findAllByOrganisationIdOrderByStartDateAsc(organisationId);
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(UUID organisationId, UUID leaveRequestId, UUID decidedByUserId, String decisionComment) {

        LeaveRequest leaveRequest = getLeaveRequest(organisationId, leaveRequestId);

        leaveRequest.approve(decidedByUserId, decisionComment);

        return leaveRequestRepository.saveAndFlush(leaveRequest);
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(UUID organisationId, UUID leaveRequestId, UUID decidedByUserId, String decisionComment) {

        LeaveRequest leaveRequest = getLeaveRequest(organisationId, leaveRequestId);

        leaveRequest.reject(decidedByUserId, decisionComment);

        return leaveRequestRepository.saveAndFlush(leaveRequest);
    }

    @Transactional
    public LeaveRequest cancelLeaveRequest(UUID organisationId, UUID leaveRequestId) {

        LeaveRequest leaveRequest = getLeaveRequest(organisationId, leaveRequestId);

        leaveRequest.cancel();

        return leaveRequestRepository.saveAndFlush(leaveRequest);
    }
}