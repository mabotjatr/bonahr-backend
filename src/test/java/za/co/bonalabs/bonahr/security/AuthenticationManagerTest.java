package za.co.bonalabs.bonahr.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthenticationManagerTest {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldAuthenticateValidUser() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        String authenticationIdentifier = organisation.getId() + ":john@example.com";

        User user = new User(organisation, "john@example.com", "John", "Doe");

        user.setPasswordHash(passwordEncoder.encode("SecretPassword123!"));

        user.setStatus(za.co.bonalabs.bonahr.entity.UserStatus.ACTIVE);

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationIdentifier, "SecretPassword123!"));

        assertThat(authentication.isAuthenticated()).isTrue();

        assertThat(authentication.getName()).isEqualTo(authenticationIdentifier);
    }

    @Test
    void shouldRejectInvalidPassword() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        User user = new User(organisation, "john@example.com", "John", "Doe");

        user.setPasswordHash(passwordEncoder.encode("CorrectPassword123!"));

        user.setStatus(za.co.bonalabs.bonahr.entity.UserStatus.ACTIVE);

        userRepository.save(user);

        assertThatThrownBy(() -> authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                "john@example.com", "WrongPassword!")))
                .isInstanceOf(AuthenticationException.class);
    }
}