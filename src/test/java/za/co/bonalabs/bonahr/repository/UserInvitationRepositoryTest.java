package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserInvitation;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserInvitationRepositoryTest {

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Test
    void shouldPersistAndFindInvitationByTokenHash() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Invitation Repository " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "invited." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        String tokenHash = "0123456789abcdef0123456789abcdef" + "0123456789abcdef0123456789abcdef";

        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);

        UserInvitation invitation = new UserInvitation(user, tokenHash, expiresAt);

        userInvitationRepository.saveAndFlush(invitation);

        UserInvitation found = userInvitationRepository.findByTokenHash(tokenHash).orElseThrow();

        assertThat(found.getId()).isNotNull();

        assertThat(found.getUser().getId()).isEqualTo(user.getId());

        assertThat(found.getTokenHash()).isEqualTo(tokenHash);

        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);

        assertThat(found.getUsedAt()).isNull();

        assertThat(found.getRevokedAt()).isNull();

        assertThat(found.getCreatedAt()).isNotNull();
    }
}