package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveRequestRequest;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.LeaveRequestRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeService employeeService;
    private final LeaveTypeService leaveTypeService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveDayCalculator leaveDayCalculator;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeService employeeService,
            LeaveTypeService leaveTypeService,
            LeaveBalanceService leaveBalanceService,
            LeaveDayCalculator leaveDayCalculator) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeService = employeeService;
        this.leaveTypeService = leaveTypeService;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveDayCalculator = leaveDayCalculator;
    }

    @Transactional
    public LeaveRequest createLeaveRequest(UUID organisationId, UUID requestedByUserId, CreateLeaveRequestRequest request) {

        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Employee employee = employeeService.getEmployee(organisationId, request.employeeId());

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalStateException("Employee is not active: " + employee.getEmployeeNumber());
        }

        LeaveType leaveType = leaveTypeService.getLeaveType(organisationId, request.leaveTypeId());

        if (!leaveType.isActive()) {
            throw new IllegalStateException("Leave type is inactive: " + leaveType.getCode());
        }

        boolean overlappingRequestExists = leaveRequestRepository.existsByOrganisationIdAndEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                organisationId,
                employee.getId(),
                List.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED),
                request.endDate(),
                request.startDate());

        if (overlappingRequestExists) {
            throw new IllegalStateException("Employee already has an overlapping leave request");
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                employee.getOrganisation(),
                employee,
                leaveType,
                request.startDate(),
                request.endDate(),
                request.dayPortion(),
                request.reason(),
                requestedByUserId);

        LeaveRequest savedLeaveRequest = leaveRequestRepository.saveAndFlush(leaveRequest);

        BigDecimal requestedDays = calculateRequestedDays(savedLeaveRequest);

        if (requestedDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Leave request must include at least one working day"
            );
        }

        leaveBalanceService.reserveLeaveDays(organisationId, savedLeaveRequest, requestedDays, requestedByUserId);

        if (!leaveType.isRequiresApproval()) {

            savedLeaveRequest.approve(requestedByUserId, "Automatically approved because approval is not required");

            savedLeaveRequest = leaveRequestRepository.saveAndFlush(savedLeaveRequest);

            leaveBalanceService.approveLeaveDays(organisationId, savedLeaveRequest, requestedDays, requestedByUserId);
        }

        return savedLeaveRequest;
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

        LeaveRequest savedLeaveRequest = leaveRequestRepository.saveAndFlush(leaveRequest);

        BigDecimal requestedDays = calculateRequestedDays(savedLeaveRequest);

        leaveBalanceService.approveLeaveDays(organisationId, leaveRequest, requestedDays, decidedByUserId);

        return savedLeaveRequest;
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(UUID organisationId, UUID leaveRequestId, UUID decidedByUserId, String decisionComment) {

        LeaveRequest leaveRequest = getLeaveRequest(organisationId, leaveRequestId);

        leaveRequest.reject(decidedByUserId, decisionComment);

        LeaveRequest savedLeaveRequest = leaveRequestRepository.saveAndFlush(leaveRequest);

        BigDecimal requestedDays = calculateRequestedDays(savedLeaveRequest);

        leaveBalanceService.releaseLeaveDays(organisationId, leaveRequest, requestedDays, decidedByUserId);

        return savedLeaveRequest;
    }

    @Transactional
    public LeaveRequest cancelLeaveRequest(UUID organisationId, UUID leaveRequestId, UUID cancelledByUserId) {

        LeaveRequest leaveRequest = getLeaveRequest(organisationId, leaveRequestId);

        leaveRequest.cancel();

        LeaveRequest savedLeaveRequest = leaveRequestRepository.saveAndFlush(leaveRequest);

        BigDecimal requestedDays = calculateRequestedDays(savedLeaveRequest);

        leaveBalanceService.releaseLeaveDays(organisationId, leaveRequest, requestedDays, cancelledByUserId);

        return savedLeaveRequest;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequests(UUID organisationId, UUID employeeId) {
        return leaveRequestRepository.findAllByOrganisationIdAndEmployeeIdOrderByStartDateAsc(organisationId, employeeId);
    }

    private BigDecimal calculateRequestedDays(LeaveRequest leaveRequest) {
        return leaveDayCalculator.calculate(leaveRequest.getStartDate(), leaveRequest.getEndDate(), leaveRequest.getDayPortion());
    }
}