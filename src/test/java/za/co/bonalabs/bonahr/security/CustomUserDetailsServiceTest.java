package za.co.bonalabs.bonahr.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.RoleRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void shouldLoadActiveUserWithRole() {


        Organisation organisation =
                organisationRepository.save(
                        new Organisation("Test Organisation")
                );

        String authenticationIdentifier =  organisation.getId() + ":john@example.com";
        Role role = roleRepository.findByName("HR_ADMIN")
                        .orElseThrow();

        User user = new User(
                organisation,
                "john@example.com",
                "John",
                "Doe"
        );

        user.setPasswordHash(
                passwordEncoder.encode("SecretPassword123!")
        );

        user.setStatus(
                za.co.bonalabs.bonahr.entity.UserStatus.ACTIVE
        );

        user.addRole(role);

        userRepository.save(user);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        authenticationIdentifier
                );

        assertThat(userDetails.getUsername())
                .isEqualTo(authenticationIdentifier);

        assertThat(userDetails.getPassword())
                .isNotEqualTo("SecretPassword123!");

        assertThat(
                passwordEncoder.matches(
                        "SecretPassword123!",
                        userDetails.getPassword()
                )
        ).isTrue();

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .contains("ROLE_HR_ADMIN");

        assertThat(userDetails.isEnabled())
                .isTrue();
    }

    @Test
    void shouldDisableSuspendedUser() {

        Organisation organisation =
                organisationRepository.save(
                        new Organisation("Test Organisation")
                );
        String authenticationIdentifier =  organisation.getId() + ":suspended@example.com";

        User user = new User(
                organisation,
                "suspended@example.com",
                "Suspended",
                "User"
        );

        user.setPasswordHash(
                passwordEncoder.encode("SecretPassword123!")
        );

        user.setStatus(
                za.co.bonalabs.bonahr.entity.UserStatus.SUSPENDED
        );

        userRepository.save(user);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        authenticationIdentifier
                );

        assertThat(userDetails.isEnabled())
                .isFalse();
    }
}