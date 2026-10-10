package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveTypeRequest;
import za.co.bonalabs.bonahr.dto.leave.LeaveTypeResponse;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeaveTypeRequest;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeaveTypeStatusRequest;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.LeaveTypeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @PostMapping
    public ResponseEntity<LeaveTypeResponse> createLeaveType(@Valid @RequestBody CreateLeaveTypeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveType leaveType = leaveTypeService.createLeaveType(details.organisationId(), request);

        LeaveTypeResponse response = toResponse(leaveType);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LeaveTypeResponse>> getLeaveTypes(Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<LeaveTypeResponse> response = leaveTypeService.getLeaveTypes(details.organisationId()).stream().map(this::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> getLeaveType(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveType leaveType = leaveTypeService.getLeaveType(details.organisationId(), id);

        return ResponseEntity.ok(toResponse(leaveType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeaveTypeResponse> updateLeaveType(@PathVariable UUID id, @Valid @RequestBody UpdateLeaveTypeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveType leaveType = leaveTypeService.updateLeaveType(details.organisationId(), id, request);

        return ResponseEntity.ok(toResponse(leaveType));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeaveTypeResponse> updateLeaveTypeStatus(@PathVariable UUID id, @Valid @RequestBody UpdateLeaveTypeStatusRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeaveType leaveType = leaveTypeService.updateLeaveTypeStatus(details.organisationId(), id, request.active());

        return ResponseEntity.ok(toResponse(leaveType));
    }

    private LeaveTypeResponse toResponse(LeaveType leaveType) {

        return new LeaveTypeResponse(
                leaveType.getId(),
                leaveType.getOrganisation().getId(),
                leaveType.getCode(),
                leaveType.getName(),
                leaveType.isPaid(),
                leaveType.isRequiresApproval(),
                leaveType.isActive(),
                leaveType.getCreatedAt(),
                leaveType.getUpdatedAt());
    }
}