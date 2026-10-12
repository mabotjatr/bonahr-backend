package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "leave_balance_transactions")
public class LeaveBalanceTransaction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_account_id", nullable = false)
    private EmployeeLeaveAccount leaveAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private LeaveBalanceTransactionType transactionType;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "reserved_after", nullable = false, precision = 12, scale = 4)
    private BigDecimal reservedAfter;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LeaveBalanceTransaction() {
    }

    public LeaveBalanceTransaction(Organisation organisation, EmployeeLeaveAccount leaveAccount, LeaveRequest leaveRequest, LeaveBalanceTransactionType transactionType, BigDecimal amount, BigDecimal balanceAfter, BigDecimal reservedAfter, UUID actorUserId, String comment) {

        if (reservedAfter == null || reservedAfter.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException("Reserved balance cannot be negative");
        }

        this.organisation = organisation;
        this.leaveAccount = leaveAccount;
        this.leaveRequest = leaveRequest;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reservedAfter = reservedAfter;
        this.actorUserId = actorUserId;
        this.comment = comment;
    }

    @PrePersist
    void prePersist() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public UUID getId() {
        return id;
    }

    public Organisation getOrganisation() {
        return organisation;
    }

    public EmployeeLeaveAccount getLeaveAccount() {
        return leaveAccount;
    }

    public LeaveRequest getLeaveRequest() {
        return leaveRequest;
    }

    public LeaveBalanceTransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getReservedAfter() {
        return reservedAfter;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getComment() {
        return comment;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}