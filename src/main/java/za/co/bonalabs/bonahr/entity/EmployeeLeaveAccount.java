package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(
        name = "employee_leave_accounts",
        uniqueConstraints = {@UniqueConstraint(name = "uq_employee_leave_accounts_employee_leave_type",
                columnNames = {"organisation_id", "employee_id", "leave_type_id"})})
public class EmployeeLeaveAccount {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_policy_id", nullable = false)
    private LeavePolicy leavePolicy;

    @Column(name = "current_balance", nullable = false, precision = 12, scale = 4)
    private BigDecimal currentBalance;

    @Column(name = "reserved_days", nullable = false, precision = 12, scale = 4)
    private BigDecimal reservedDays;

    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EmployeeLeaveAccount() {
    }

    public EmployeeLeaveAccount(Organisation organisation, Employee employee, LeaveType leaveType, LeavePolicy leavePolicy) {
        this.organisation = organisation;
        this.employee = employee;
        this.leaveType = leaveType;
        this.leavePolicy = leavePolicy;
        this.currentBalance = BigDecimal.ZERO;
        this.reservedDays = BigDecimal.ZERO;
    }

    public void reserveDays(BigDecimal days) {

        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException("Reserved days must be greater than zero");
        }

        BigDecimal projectedBalance = currentBalance.subtract(reservedDays).subtract(days);

        BigDecimal minimumAllowedBalance = leavePolicy.isAllowNegative() ? leavePolicy.getNegativeLimitDays().negate() : BigDecimal.ZERO;

        if (projectedBalance.compareTo(minimumAllowedBalance) < 0) {

            throw new IllegalStateException("Leave reservation exceeds allowed balance");
        }

        this.reservedDays = this.reservedDays.add(days);
    }

    public void releaseReservedDays(BigDecimal days) {

        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Released days must be greater than zero" );
        }

        if (days.compareTo(reservedDays) > 0) {
            throw new IllegalStateException("Cannot release more days than are reserved");
        }

        this.reservedDays = this.reservedDays.subtract(days);
    }

    public void approveReservedDays(BigDecimal days) {

        if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException("Approved days must be greater than zero");
        }

        if (days.compareTo(reservedDays) > 0) {

            throw new IllegalStateException("Cannot approve more days than are reserved");
        }

        this.reservedDays = this.reservedDays.subtract(days);
        this.currentBalance = this.currentBalance.subtract(days);
    }

    public void applyAccrual(BigDecimal amount, LocalDate accrualDate) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException("Accrual amount must be greater than zero");
        }

        if (accrualDate == null) {

            throw new IllegalArgumentException("Accrual date is required");
        }

        if (lastAccrualDate != null && !accrualDate.isAfter(lastAccrualDate)) {

            throw new IllegalStateException("Accrual date must be after the last accrual date");
        }

        this.currentBalance = this.currentBalance.add(amount);

        this.lastAccrualDate = accrualDate;
    }

    public void applyManualAdjustment(BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {

            throw new IllegalArgumentException("Manual adjustment amount cannot be zero");
        }

        BigDecimal adjustedBalance = this.currentBalance.add(amount);

        BigDecimal effectiveProjectedBalance = adjustedBalance.subtract(this.reservedDays);

        BigDecimal minimumAllowedBalance = leavePolicy.isAllowNegative() ? leavePolicy.getNegativeLimitDays().negate() : BigDecimal.ZERO;

        if (effectiveProjectedBalance.compareTo(minimumAllowedBalance) < 0) {

            throw new IllegalStateException("Manual adjustment exceeds allowed negative balance");
        }

        this.currentBalance = adjustedBalance;
    }

    @PrePersist
    void prePersist() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LeavePolicy getLeavePolicy() {
        return leavePolicy;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getReservedDays() {
        return reservedDays;
    }

    public LocalDate getLastAccrualDate() {
        return lastAccrualDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}