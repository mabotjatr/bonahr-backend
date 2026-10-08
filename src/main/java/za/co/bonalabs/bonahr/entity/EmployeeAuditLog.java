package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "employee_audit_log")
public class EmployeeAuditLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmployeeAuditAction action;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected EmployeeAuditLog() {
        // Required by JPA
    }

    public EmployeeAuditLog(Organisation organisation, Employee employee, EmployeeAuditAction action, UUID actorUserId, Map<String, Object> changes) {
        this.organisation = organisation;
        this.employee = employee;
        this.action = action;
        this.actorUserId = actorUserId;
        this.changes = changes;
    }

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
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

    public EmployeeAuditAction getAction() {
        return action;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}