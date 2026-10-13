package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeavePolicyStatusRequest;
import za.co.bonalabs.bonahr.dto.leave.UpdateLeavePolicyRequest;
import za.co.bonalabs.bonahr.dto.leave.CreateLeavePolicyRequest;
import za.co.bonalabs.bonahr.dto.leave.LeavePolicyResponse;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.LeavePolicyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-policies")
public class LeavePolicyController {

    private final LeavePolicyService leavePolicyService;

    public LeavePolicyController(LeavePolicyService leavePolicyService) {
        this.leavePolicyService = leavePolicyService;
    }

    @PostMapping
    public ResponseEntity<LeavePolicyResponse> createLeavePolicy(@Valid @RequestBody CreateLeavePolicyRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeavePolicy leavePolicy = leavePolicyService.createLeavePolicy(details.organisationId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(leavePolicy));
    }

    @GetMapping
    public ResponseEntity<List<LeavePolicyResponse>> getLeavePolicies(Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<LeavePolicyResponse> response = leavePolicyService.getLeavePolicies(details.organisationId()).stream().map(this::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeavePolicyResponse> getLeavePolicy(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeavePolicy leavePolicy = leavePolicyService.getLeavePolicy(details.organisationId(), id);

        return ResponseEntity.ok(toResponse(leavePolicy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeavePolicyResponse> updateLeavePolicy(@PathVariable UUID id, @Valid @RequestBody UpdateLeavePolicyRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeavePolicy leavePolicy = leavePolicyService.updateLeavePolicy(details.organisationId(), id, request);

        return ResponseEntity.ok(toResponse(leavePolicy));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LeavePolicyResponse> updateLeavePolicyStatus(@PathVariable UUID id, @Valid @RequestBody UpdateLeavePolicyStatusRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        LeavePolicy leavePolicy = leavePolicyService.updateLeavePolicyStatus(details.organisationId(), id, request.active());

        return ResponseEntity.ok(toResponse(leavePolicy));
    }

    private LeavePolicyResponse toResponse(LeavePolicy leavePolicy) {
        return new LeavePolicyResponse(
                leavePolicy.getId(),
                leavePolicy.getLeaveType().getId(),
                leavePolicy.getLeaveType().getCode(),
                leavePolicy.getLeaveType().getName(),
                leavePolicy.getAccrualMethod(),
                leavePolicy.getAccrualRate(),
                leavePolicy.isAllowNegative(),
                leavePolicy.getNegativeLimitDays(),
                leavePolicy.isActive());
    }
}