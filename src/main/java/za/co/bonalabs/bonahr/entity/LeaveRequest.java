package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_portion", nullable = false, length = 30)
    private LeaveDayPortion dayPortion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaveRequestStatus status;

    @Column(length = 1000)
    private String reason;

    @Column(name = "decision_comment", length = 1000)
    private String decisionComment;

    @Column(name = "requested_by_user_id")
    private UUID requestedByUserId;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected LeaveRequest() {
    }

    public LeaveRequest(
            Organisation organisation,
            Employee employee,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            UUID requestedByUserId) {
        this(organisation, employee, leaveType, startDate, endDate, LeaveDayPortion.FULL_DAY, reason, requestedByUserId);
    }

    public LeaveRequest(
            Organisation organisation,
            Employee employee,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            LeaveDayPortion dayPortion,
            String reason,
            UUID requestedByUserId) {
        this.organisation = organisation;
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayPortion = dayPortion;
        this.reason = reason;
        this.requestedByUserId = requestedByUserId;
        this.status = LeaveRequestStatus.PENDING;
    }

    public void approve(UUID decidedByUserId, String decisionComment) {

        if (status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be approved" );
        }

        this.status = LeaveRequestStatus.APPROVED;
        this.decidedByUserId = decidedByUserId;
        this.decisionComment = decisionComment;
        this.decidedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void reject(UUID decidedByUserId, String decisionComment) {

        if (status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be rejected");
        }

        this.status = LeaveRequestStatus.REJECTED;
        this.decidedByUserId = decidedByUserId;
        this.decisionComment = decisionComment;
        this.decidedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void cancel() {

        if (status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException( "Only pending leave requests can be cancelled");
        }

        this.status = LeaveRequestStatus.CANCELLED;
    }

    @PrePersist
    void prePersist() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (status == null) {
            status = LeaveRequestStatus.PENDING;
        }

        if (requestedAt == null) {
            requestedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (dayPortion == null) {
            dayPortion = LeaveDayPortion.FULL_DAY;
        }

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LeaveDayPortion getDayPortion() {
        return dayPortion;
    }

    public LeaveRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public UUID getRequestedByUserId() {
        return requestedByUserId;
    }

    public UUID getDecidedByUserId() {
        return decidedByUserId;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}