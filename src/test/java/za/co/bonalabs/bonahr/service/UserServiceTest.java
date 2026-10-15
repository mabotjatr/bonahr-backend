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
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void shouldHashUserPassword() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        User user = userRepository.save(new User(organisation, "john@example.com", "John", "Doe"));

        User updated = userService.setPassword(user.getId(), "SecretPassword123!");

        assertThat(updated.getPasswordHash()).isNotNull();

        assertThat(updated.getPasswordHash()).isNotEqualTo("SecretPassword123!");

        assertThat(passwordEncoder.matches("SecretPassword123!", updated.getPasswordHash())).isTrue();
    }

    @Test
    void shouldResolveLinkedEmployeeForUserWithinOrganisation() {

        Organisation organisation = organisationRepository.save(new Organisation("Employee Resolution Organisation"));

        Employee employee = employeeRepository.save(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        User user = new User(organisation, "john.employee@example.com", "John", "Doe");

        user.linkEmployee(employee);

        user = userRepository.saveAndFlush(user);

        Employee resolved = userService.resolveEmployee(organisation.getId(), user.getId());

        assertThat(resolved.getId()).isEqualTo(employee.getId());

        assertThat(resolved.getOrganisation().getId()).isEqualTo(organisation.getId());
    }

    @Test
    void shouldNotResolveEmployeeForUserFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.save(new Organisation("Organisation A"));

        Organisation organisationB = organisationRepository.save(new Organisation("Organisation B"));

        Employee employee = employeeRepository.save(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        User user = new User(organisationA, "john.cross.tenant@example.com", "John", "Doe");

        user.linkEmployee(employee);

        user = userRepository.saveAndFlush(user);

        UUID userId = user.getId();

        assertThatThrownBy(() -> userService.resolveEmployee(organisationB.getId(), userId)).isInstanceOf(IllegalArgumentException.class).hasMessage("User not found: " + userId);
    }

    @Test
    void shouldRejectResolvingEmployeeWhenUserHasNoLinkedEmployee() {

        Organisation organisation = organisationRepository.save(new Organisation("Unlinked User Organisation"));

        User user = userRepository.saveAndFlush(new User(organisation, "unlinked@example.com", "Jane", "Doe"));

        UUID userId = user.getId();

        assertThatThrownBy(() -> userService.resolveEmployee(organisation.getId(), userId)).isInstanceOf(IllegalStateException.class).hasMessage("User is not linked to an employee");
    }
}