package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSaveUserForOrganisation() {

        Organisation organisation = new Organisation("Test Organisation");

        organisation = organisationRepository.save(organisation);

        User user = new User(organisation, "john@example.com", "John", "Doe");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();

        assertThat(saved.getOrganisation().getId()).isEqualTo(organisation.getId());

        assertThat(saved.getEmail()).isEqualTo("john@example.com");

        assertThat(saved.getStatus()).isEqualTo(UserStatus.INVITED);
    }

    @Test
    void shouldFindUserWithinOrganisation() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        User user = new User(organisation, "john@example.com", "John", "Doe");

        userRepository.save(user);

        Optional<User> result = userRepository.findByOrganisationIdAndEmail(organisation.getId(), "john@example.com");

        assertThat(result).isPresent();

        assertThat(result.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldAllowSameEmailForDifferentOrganisations() {

        Organisation organisationA = organisationRepository.save(new Organisation("Organisation A"));

        Organisation organisationB = organisationRepository.save(new Organisation("Organisation B"));

        User userA = new User(organisationA, "john@example.com", "John", "Doe");

        User userB = new User(organisationB, "john@example.com", "John", "Doe");

        userRepository.save(userA);
        userRepository.save(userB);

        assertThat(userRepository.existsByOrganisationIdAndEmail(organisationA.getId(), "john@example.com")).isTrue();

        assertThat(userRepository.existsByOrganisationIdAndEmail(organisationB.getId(), "john@example.com")).isTrue();
    }

    @Test
    void shouldAssignMultipleRolesToUser() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        Role hrAdmin = roleRepository.findByName("HR_ADMIN").orElseThrow();

        Role payrollAdmin = roleRepository.findByName("PAYROLL_ADMIN").orElseThrow();

        User user = new User(organisation, "john@example.com", "John", "Doe");

        user.addRole(hrAdmin);
        user.addRole(payrollAdmin);

        User saved = userRepository.save(user);

        assertThat(saved.getRoles()).hasSize(2);

        assertThat(saved.getRoles()).extracting(Role::getName).containsExactlyInAnyOrder("HR_ADMIN", "PAYROLL_ADMIN");
    }
}