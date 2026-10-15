package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.entity.Employee;

import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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

    @Test
    void shouldLinkUserToEmployeeWithinSameOrganisation() {

        Organisation organisation = organisationRepository.save(new Organisation("Employee Link Organisation"));

        Employee employee = employeeRepository.save(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        User user = new User(organisation, "john.employee@example.com", "John", "Doe");

        user.linkEmployee(employee);

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getEmployee()).isNotNull();

        assertThat(saved.getEmployee().getId()).isEqualTo(employee.getId());
    }

    @Test
    void shouldRejectLinkingUserToEmployeeFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.save(new Organisation("User Organisation"));

        Organisation organisationB = organisationRepository.save(new Organisation("Employee Organisation"));

        Employee employee = employeeRepository.save(new Employee(organisationB, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = new User(organisationA, "john@example.com", "John", "Doe");

        assertThatThrownBy(() -> user.linkEmployee(employee)).isInstanceOf(IllegalArgumentException.class).hasMessage("User and employee must belong to the same organisation");
    }
}