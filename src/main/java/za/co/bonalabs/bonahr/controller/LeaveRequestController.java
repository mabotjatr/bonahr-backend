package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveRequestRequest;
import za.co.bonalabs.bonahr.dto.leave.LeaveDecisionRequest;
import za.co.bonalabs.bonahr.dto.leave.LeaveRequestResponse;
import za.co.bonalabs.bonahr.entity.LeaveRequest;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.LeaveRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(@Valid @RequestBody CreateLeaveRequestRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(details.organisationId(), details.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(leaveRequest));
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveRequests(Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<LeaveRequestResponse> response = leaveRequestService.getLeaveRequests(details.organisationId()).stream().map(this::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequest(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveRequest leaveRequest = leaveRequestService.getLeaveRequest(details.organisationId(), id);

        return ResponseEntity.ok(toResponse(leaveRequest));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(@PathVariable UUID id, @Valid @RequestBody LeaveDecisionRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveRequest leaveRequest = leaveRequestService.approveLeaveRequest(details.organisationId(), id, details.userId(), request.comment());

        return ResponseEntity.ok(toResponse(leaveRequest));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(@PathVariable UUID id, @Valid @RequestBody LeaveDecisionRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveRequest leaveRequest = leaveRequestService.rejectLeaveRequest(details.organisationId(), id, details.userId(), request.comment());

        return ResponseEntity.ok(toResponse(leaveRequest));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<LeaveRequestResponse> cancelLeaveRequest(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveRequest leaveRequest = leaveRequestService.cancelLeaveRequest(details.organisationId(), id, details.userId());

        return ResponseEntity.ok(toResponse(leaveRequest));
    }

    private LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        return new LeaveRequestResponse(
                leaveRequest.getId(),
                leaveRequest.getOrganisation().getId(),
                leaveRequest.getEmployee().getId(),
                leaveRequest.getLeaveType().getId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getStatus(),
                leaveRequest.getReason(),
                leaveRequest.getDecisionComment(),
                leaveRequest.getRequestedByUserId(),
                leaveRequest.getDecidedByUserId(),
                leaveRequest.getRequestedAt(),
                leaveRequest.getDecidedAt(),
                leaveRequest.getCreatedAt(),
                leaveRequest.getUpdatedAt());
    }

}