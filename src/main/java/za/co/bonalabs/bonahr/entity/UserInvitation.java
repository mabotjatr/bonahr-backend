package za.co.bonalabs.bonahr.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_invitations", uniqueConstraints = {@UniqueConstraint(name = "uq_user_invitations_token_hash", columnNames = "token_hash")})
public class UserInvitation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserInvitation() {
        // Required by JPA
    }

    public UserInvitation(User user, String tokenHash, OffsetDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsableAt(OffsetDateTime now) {
        return usedAt == null
                && revokedAt == null
                && now.isBefore(expiresAt);
    }

    public void markUsed(OffsetDateTime usedAt) {

        if (!isUsableAt(usedAt)) {
            throw new IllegalStateException("Invitation is no longer usable");
        }

        this.usedAt = usedAt;
    }

    public void revoke(OffsetDateTime revokedAt) {

        if (usedAt != null) {
            throw new IllegalStateException("Used invitation cannot be revoked");
        }

        if (this.revokedAt != null) {
            throw new IllegalStateException("Invitation is already revoked");
        }

        this.revokedAt = revokedAt;
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

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getUsedAt() {
        return usedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}