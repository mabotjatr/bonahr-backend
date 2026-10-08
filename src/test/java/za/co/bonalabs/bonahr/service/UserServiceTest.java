package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldHashUserPassword() {

        Organisation organisation =
                organisationRepository.save(
                        new Organisation("Test Organisation")
                );

        User user = userRepository.save(
                new User(
                        organisation,
                        "john@example.com",
                        "John",
                        "Doe"
                )
        );

        User updated =
                userService.setPassword(
                        user.getId(),
                        "SecretPassword123!"
                );

        assertThat(updated.getPasswordHash())
                .isNotNull();

        assertThat(updated.getPasswordHash())
                .isNotEqualTo("SecretPassword123!");

        assertThat(
                passwordEncoder.matches(
                        "SecretPassword123!",
                        updated.getPasswordHash()
                )
        ).isTrue();
    }
}