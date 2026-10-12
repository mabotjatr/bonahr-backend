package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.EmployeeLeaveAccount;
import za.co.bonalabs.bonahr.entity.LeaveBalanceTransaction;
import za.co.bonalabs.bonahr.entity.LeaveBalanceTransactionType;
import za.co.bonalabs.bonahr.entity.LeaveRequest;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;
import za.co.bonalabs.bonahr.repository.LeaveBalanceTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LeaveBalanceService {

    private final EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    private final LeaveBalanceTransactionRepository leaveBalanceTransactionRepository;

    public LeaveBalanceService(EmployeeLeaveAccountRepository employeeLeaveAccountRepository, LeaveBalanceTransactionRepository leaveBalanceTransactionRepository) {
        this.employeeLeaveAccountRepository = employeeLeaveAccountRepository;

        this.leaveBalanceTransactionRepository = leaveBalanceTransactionRepository;
    }

    @Transactional
    public void applyAccrual(UUID organisationId, UUID accountId, BigDecimal amount, LocalDate accrualDate, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, accountId);

        account.applyAccrual(amount, accrualDate);

        employeeLeaveAccountRepository.saveAndFlush(account);

        recordTransaction(account, null, LeaveBalanceTransactionType.ACCRUAL, amount, actorUserId, "Leave accrual");
    }

    @Transactional
    public void reserveLeaveDays(UUID organisationId, UUID accountId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, accountId);

        reserveLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void reserveLeaveDays(UUID organisationId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId());

        reserveLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void releaseLeaveDays(UUID organisationId, UUID accountId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, accountId);

        releaseLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void releaseLeaveDays(UUID organisationId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId());

        releaseLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void approveLeaveDays(UUID organisationId, UUID accountId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, accountId);

        approveLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void approveLeaveDays(UUID organisationId, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId());

        approveLeaveDays(account, leaveRequest, days, actorUserId);
    }

    @Transactional
    public void applyManualAdjustment(UUID organisationId, UUID accountId, BigDecimal amount, UUID actorUserId, String comment) {

        EmployeeLeaveAccount account = getLeaveAccount(organisationId, accountId);

        account.applyManualAdjustment(amount);

        employeeLeaveAccountRepository.saveAndFlush(account);

        recordTransaction(account, null, LeaveBalanceTransactionType.MANUAL_ADJUSTMENT, amount, actorUserId, comment);
    }

    @Transactional(readOnly = true)
    public EmployeeLeaveAccount getLeaveAccount(UUID organisationId, UUID employeeId, UUID leaveTypeId) {

        return employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(
                organisationId,
                employeeId,
                leaveTypeId).orElseThrow(() -> new ResourceNotFoundException("Leave account not found for employee: " + employeeId + " and leave type: " + leaveTypeId));
    }

    private void releaseLeaveDays(EmployeeLeaveAccount account, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        validateLeaveRequestMatchesAccount(account, leaveRequest);

        account.releaseReservedDays(days);

        employeeLeaveAccountRepository.saveAndFlush(account);

        recordTransaction(account, leaveRequest, LeaveBalanceTransactionType.LEAVE_RESERVATION_RELEASED, days.negate(), actorUserId, "Leave reservation released");
    }

    private void approveLeaveDays(EmployeeLeaveAccount account, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        validateLeaveRequestMatchesAccount(account, leaveRequest);

        account.approveReservedDays(days);

        employeeLeaveAccountRepository.saveAndFlush(account);

        recordTransaction(account, leaveRequest, LeaveBalanceTransactionType.LEAVE_APPROVED, days.negate(), actorUserId, "Leave approved");
    }

    private EmployeeLeaveAccount getLeaveAccount(UUID organisationId, UUID accountId) {

        return employeeLeaveAccountRepository.findByIdAndOrganisationId(
                accountId,
                organisationId).orElseThrow(() -> new ResourceNotFoundException("Leave account not found: " + accountId));
    }

    private void reserveLeaveDays(EmployeeLeaveAccount account, LeaveRequest leaveRequest, BigDecimal days, UUID actorUserId) {

        validateLeaveRequestMatchesAccount(account, leaveRequest);

        account.reserveDays(days);

        employeeLeaveAccountRepository.saveAndFlush(account);

        recordTransaction(account, leaveRequest, LeaveBalanceTransactionType.LEAVE_RESERVED, days, actorUserId, "Leave reserved");
    }

    private void validateLeaveRequestMatchesAccount(EmployeeLeaveAccount account, LeaveRequest leaveRequest) {

        if (!sameEntity(account.getEmployee().getId(), leaveRequest.getEmployee().getId(), account.getEmployee(), leaveRequest.getEmployee())) {
            throw new IllegalArgumentException("Leave request employee does not match leave account employee");
        }

        if (!sameEntity(account.getLeaveType().getId(), leaveRequest.getLeaveType().getId(), account.getLeaveType(), leaveRequest.getLeaveType())) {
            throw new IllegalArgumentException("Leave request type does not match leave account type");
        }
    }

    private boolean sameEntity(UUID firstId, UUID secondId, Object firstEntity, Object secondEntity) {

        if (firstId != null && secondId != null) {
            return firstId.equals(secondId);
        }

        return firstEntity == secondEntity;
    }

    private void recordTransaction(EmployeeLeaveAccount account, LeaveRequest leaveRequest, LeaveBalanceTransactionType transactionType, BigDecimal amount, UUID actorUserId, String comment) {

        LeaveBalanceTransaction transaction = new LeaveBalanceTransaction(
                account.getOrganisation(),
                account,
                leaveRequest,
                transactionType,
                amount,
                account.getCurrentBalance(),
                account.getReservedDays(),
                actorUserId,
                comment);

        leaveBalanceTransactionRepository.saveAndFlush(transaction);
    }
}