package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.dto.leave.CreateEmployeeLeaveAccountRequest;
import za.co.bonalabs.bonahr.dto.leave.EmployeeLeaveAccountResponse;
import za.co.bonalabs.bonahr.dto.leave.LeaveBalanceTransactionResponse;
import za.co.bonalabs.bonahr.dto.leave.ManualLeaveBalanceAdjustmentRequest;
import za.co.bonalabs.bonahr.entity.EmployeeLeaveAccount;
import za.co.bonalabs.bonahr.entity.LeaveBalanceTransaction;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.EmployeeLeaveAccountService;
import za.co.bonalabs.bonahr.service.LeaveBalanceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave-accounts")
public class EmployeeLeaveAccountController {

    private final EmployeeLeaveAccountService employeeLeaveAccountService;
    private final LeaveBalanceService leaveBalanceService;

    public EmployeeLeaveAccountController(EmployeeLeaveAccountService employeeLeaveAccountService, LeaveBalanceService leaveBalanceService) {
        this.employeeLeaveAccountService = employeeLeaveAccountService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @PostMapping
    public ResponseEntity<EmployeeLeaveAccountResponse> createEmployeeLeaveAccount(@Valid @RequestBody CreateEmployeeLeaveAccountRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        EmployeeLeaveAccount account = employeeLeaveAccountService.createEmployeeLeaveAccount(details.organisationId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeLeaveAccountResponse>> getEmployeeLeaveAccounts(Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<EmployeeLeaveAccountResponse> response = employeeLeaveAccountService.getEmployeeLeaveAccounts(details.organisationId()).stream().map(this::toResponse).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeLeaveAccountResponse> getEmployeeLeaveAccount(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        EmployeeLeaveAccount account = employeeLeaveAccountService.getEmployeeLeaveAccount(details.organisationId(), id);

        return ResponseEntity.ok(toResponse(account));
    }

    @PostMapping("/{id}/adjustments")
    public ResponseEntity<EmployeeLeaveAccountResponse> applyManualAdjustment(
            @PathVariable UUID id,
            @Valid @RequestBody ManualLeaveBalanceAdjustmentRequest request,
            Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        leaveBalanceService.applyManualAdjustment(details.organisationId(), id, request.amount(), details.userId(), request.comment());

        EmployeeLeaveAccount account = employeeLeaveAccountService.getEmployeeLeaveAccount(details.organisationId(), id);

        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<LeaveBalanceTransactionResponse>> getLeaveBalanceTransactions(@PathVariable UUID id, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        List<LeaveBalanceTransactionResponse> response = leaveBalanceService.getTransactions(details.organisationId(), id).stream().map(this::toTransactionResponse).toList();

        return ResponseEntity.ok(response);
    }

    private LeaveBalanceTransactionResponse toTransactionResponse(LeaveBalanceTransaction transaction) {

        UUID leaveRequestId = transaction.getLeaveRequest() == null ? null : transaction.getLeaveRequest().getId();

        return new LeaveBalanceTransactionResponse(
                transaction.getId(),
                transaction.getLeaveAccount().getId(),
                leaveRequestId,
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getReservedAfter(),
                transaction.getActorUserId(),
                transaction.getComment(),
                transaction.getCreatedAt()
        );
    }

    private EmployeeLeaveAccountResponse toResponse(EmployeeLeaveAccount account) {

        String employeeName = account.getEmployee().getFirstName() + " " + account.getEmployee().getLastName();

        return new EmployeeLeaveAccountResponse(
                account.getId(),
                account.getEmployee().getId(),
                account.getEmployee().getEmployeeNumber(),
                employeeName,
                account.getLeaveType().getId(),
                account.getLeaveType().getCode(),
                account.getLeaveType().getName(),
                account.getLeavePolicy().getId(),
                account.getCurrentBalance(),
                account.getReservedDays(),
                account.getLastAccrualDate());
    }
}