package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "leave_policies", uniqueConstraints = {@UniqueConstraint(name = "uq_leave_policies_organisation_leave_type", columnNames = {"organisation_id", "leave_type_id"})})
public class LeavePolicy {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "accrual_method", nullable = false, length = 30)
    private LeaveAccrualMethod accrualMethod;

    @Column(name = "accrual_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal accrualRate;

    @Column(name = "allow_negative", nullable = false)
    private boolean allowNegative;

    @Column(name = "negative_limit_days", nullable = false, precision = 12, scale = 4)
    private BigDecimal negativeLimitDays;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LeavePolicy() {
    }

    public LeavePolicy(Organisation organisation, LeaveType leaveType, LeaveAccrualMethod accrualMethod, BigDecimal accrualRate, boolean allowNegative, BigDecimal negativeLimitDays) {

        if (negativeLimitDays == null || negativeLimitDays.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("Negative limit cannot be less than zero");
        }

        if (accrualRate == null || accrualRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Accrual rate must be greater than zero");
        }

        if (!allowNegative && negativeLimitDays.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Negative limit must be zero when negative leave is disabled" );
        }

        this.organisation = organisation;
        this.leaveType = leaveType;
        this.accrualMethod = accrualMethod;
        this.accrualRate = accrualRate;
        this.allowNegative = allowNegative;
        this.negativeLimitDays = negativeLimitDays;
        this.active = true;
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

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LeaveAccrualMethod getAccrualMethod() {
        return accrualMethod;
    }

    public BigDecimal getAccrualRate() {
        return accrualRate;
    }

    public boolean isAllowNegative() {
        return allowNegative;
    }

    public BigDecimal getNegativeLimitDays() {
        return negativeLimitDays;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}