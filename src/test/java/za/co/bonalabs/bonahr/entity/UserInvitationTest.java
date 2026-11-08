package za.co.bonalabs.bonahr.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserInvitationTest {

    @Test
    void shouldBeUsableBeforeExpiry() {

        Organisation organisation = new Organisation("Invitation Organisation");

        User user = new User(organisation, "jane@example.com", "Jane", "Doe");

        OffsetDateTime now = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "0123456789abcdef0123456789abcdef" + "0123456789abcdef0123456789abcdef", now.plusHours(24));

        assertThat(invitation.isUsableAt(now)).isTrue();
    }

    @Test
    void shouldNotBeUsableAtOrAfterExpiry() {

        Organisation organisation = new Organisation("Expired Invitation Organisation");

        User user = new User(organisation, "expired@example.com", "Jane", "Doe");

        OffsetDateTime expiresAt = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "abcdef0123456789abcdef0123456789" + "abcdef0123456789abcdef0123456789", expiresAt);

        assertThat(invitation.isUsableAt(expiresAt)).isFalse();

        assertThat(invitation.isUsableAt(expiresAt.plusSeconds(1))).isFalse();
    }

    @Test
    void shouldBecomeUnusableAfterBeingMarkedUsed() {

        Organisation organisation = new Organisation("Used Invitation Organisation");

        User user = new User(organisation, "used@example.com", "Jane", "Doe");

        OffsetDateTime now = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "fedcba9876543210fedcba9876543210" + "fedcba9876543210fedcba9876543210", now.plusHours(24));

        invitation.markUsed(now);

        assertThat(invitation.getUsedAt()).isEqualTo(now);

        assertThat(invitation.isUsableAt(now.plusSeconds(1))).isFalse();
    }

    @Test
    void shouldRejectMarkingExpiredInvitationAsUsed() {

        Organisation organisation = new Organisation("Expired Use Invitation Organisation");

        User user = new User(organisation, "expired.use@example.com", "Jane", "Doe");

        OffsetDateTime expiresAt = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "00112233445566778899aabbccddeeff" + "00112233445566778899aabbccddeeff", expiresAt);

        assertThatThrownBy(() -> invitation.markUsed(expiresAt.plusSeconds(1))).isInstanceOf(IllegalStateException.class).hasMessage("Invitation is no longer usable");

        assertThat(invitation.getUsedAt()).isNull();
    }

    @Test
    void shouldBecomeUnusableAfterBeingRevoked() {

        Organisation organisation = new Organisation("Revoked Invitation Organisation");

        User user = new User(organisation, "revoked@example.com", "Jane", "Doe");

        OffsetDateTime now = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "ffeeddccbbaa99887766554433221100" + "ffeeddccbbaa99887766554433221100", now.plusHours(24));

        invitation.revoke(now);

        assertThat(invitation.getRevokedAt()).isEqualTo(now);

        assertThat(invitation.isUsableAt(now.plusSeconds(1))).isFalse();
    }

    @Test
    void shouldRejectRevokingUsedInvitation() {

        Organisation organisation = new Organisation("Used Then Revoked Invitation Organisation");

        User user = new User(organisation, "used.revoked@example.com", "Jane", "Doe");

        OffsetDateTime now = OffsetDateTime.now();

        UserInvitation invitation = new UserInvitation(user, "11223344556677889900aabbccddeeff" + "11223344556677889900aabbccddeeff", now.plusHours(24));

        invitation.markUsed(now);

        assertThatThrownBy(() -> invitation.revoke(now.plusSeconds(1))).isInstanceOf(IllegalStateException.class).hasMessage("Used invitation cannot be revoked");

        assertThat(invitation.getRevokedAt()).isNull();
    }
}