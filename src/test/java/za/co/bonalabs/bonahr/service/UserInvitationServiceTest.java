package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserInvitation;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.exception.InvalidUserInvitationException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserInvitationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserInvitationServiceTest {

    @Autowired
    private UserInvitationService userInvitationService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateInvitationWithHashedToken() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invitation Service " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "invitation.service." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation created = userInvitationService.createInvitation(organisation.getId(), user.getId());

        assertThat(created.rawToken()).isNotBlank();

        assertThat(created.invitation().getId()).isNotNull();

        assertThat(created.invitation().getUser().getId()).isEqualTo(user.getId());

        String expectedHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(created.rawToken().getBytes(StandardCharsets.UTF_8)));

        assertThat(created.invitation().getTokenHash()).isEqualTo(expectedHash);

        assertThat(created.invitation().getTokenHash()).isNotEqualTo(created.rawToken());

        UserInvitation persisted = userInvitationRepository.findByTokenHash(expectedHash).orElseThrow();

        assertThat(persisted.getId()).isEqualTo(created.invitation().getId());

        assertThat(persisted.getExpiresAt()).isAfter(persisted.getCreatedAt());
    }

    @Test
    void shouldRejectCreatingInvitationForActiveUser() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Active User Invitation " + UUID.randomUUID()));

        User user = new User(organisation, "active.invitation." + UUID.randomUUID() + "@example.com", "Jane", "Doe");

        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.saveAndFlush(user);

        UUID userId = user.getId();

        assertThatThrownBy(() -> userInvitationService.createInvitation(organisation.getId(), userId)).isInstanceOf(IllegalStateException.class).hasMessage("Only invited users can receive a setup invitation");
    }

    @Test
    void shouldRevokePreviousInvitationWhenCreatingNewOne() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invitation Reissue " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "reissue." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation first = userInvitationService.createInvitation(organisation.getId(), user.getId());

        UserInvitationService.CreatedInvitation second = userInvitationService.createInvitation(organisation.getId(), user.getId());

        UserInvitation persistedFirst = userInvitationRepository.findById(first.invitation().getId()).orElseThrow();

        assertThat(persistedFirst.getRevokedAt()).isNotNull();

        assertThat(persistedFirst.isUsableAt(OffsetDateTime.now())).isFalse();

        assertThat(second.invitation().getRevokedAt()).isNull();

        assertThat(second.invitation().isUsableAt(OffsetDateTime.now())).isTrue();

        assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
    }

    @Test
    void shouldConsumeInvitationAndActivateUser() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Consume Invitation " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "consume." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation created = userInvitationService.createInvitation(organisation.getId(), user.getId());

        String newPassword = "StrongPassword123!";

        User activatedUser = userInvitationService.consumeInvitation(created.rawToken(), newPassword);

        assertThat(activatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(activatedUser.getPasswordHash()).isNotBlank();

        assertThat(passwordEncoder.matches(newPassword, activatedUser.getPasswordHash())).isTrue();

        UserInvitation persistedInvitation = userInvitationRepository.findById(created.invitation().getId()).orElseThrow();

        assertThat(persistedInvitation.getUsedAt()).isNotNull();

        assertThat(persistedInvitation.isUsableAt(OffsetDateTime.now())).isFalse();
    }

    @Test
    void shouldRejectConsumingRevokedInvitation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Revoked Invitation Consumption " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "revoked.consume." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation first = userInvitationService.createInvitation(organisation.getId(), user.getId());

        userInvitationService.createInvitation(organisation.getId(), user.getId());

        assertThatThrownBy(() -> userInvitationService.consumeInvitation(first.rawToken(), "StrongPassword123!"))
                .isInstanceOf(InvalidUserInvitationException.class).hasMessage("Invitation is no longer usable");
    }

    @Test
    void shouldRejectConsumingInvitationTwice() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invitation Double Consumption " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "double.consume." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation created = userInvitationService.createInvitation(organisation.getId(), user.getId());

        userInvitationService.consumeInvitation(created.rawToken(), "StrongPassword123!");

        assertThatThrownBy(() -> userInvitationService.consumeInvitation(created.rawToken(), "AnotherPassword456!"))
                .isInstanceOf(InvalidUserInvitationException.class).hasMessage("Invitation is no longer usable");
    }

    @Test
    void shouldRejectInvalidInvitationToken() {

        assertThatThrownBy(() -> userInvitationService.consumeInvitation("this-is-not-a-valid-invitation-token", "StrongPassword123!"))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Invitation not found");
    }

    @Test
    void shouldRejectConsumingInvitationWhenUserIsNoLongerInvited() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invitation User Status " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "status.changed." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation invitation = userInvitationService.createInvitation(organisation.getId(), user.getId());

        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> userInvitationService.consumeInvitation(invitation.rawToken(), "StrongPassword123!"))
                .isInstanceOf(InvalidUserInvitationException.class).hasMessage("Invitation is no longer usable");
    }
}