package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.InvalidUserProvisioningException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
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

    @Test
    void shouldLinkUserToEmployeeWithinSameOrganisation() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Employee Linking Organisation " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe"));

        User user = userRepository.saveAndFlush(new User(organisation, "john.link." + UUID.randomUUID() + "@example.com", "John", "Doe"));

        User linkedUser = userService.linkEmployee(organisation.getId(), user.getId(), employee.getId());

        assertThat(linkedUser.getEmployee()).isNotNull();

        assertThat(linkedUser.getEmployee().getId()).isEqualTo(employee.getId());

        User persisted = userRepository.findById(user.getId()).orElseThrow();

        assertThat(persisted.getEmployee()).isNotNull();

        assertThat(persisted.getEmployee().getId()).isEqualTo(employee.getId());
    }

    @Test
    void shouldRejectLinkingUserToEmployeeFromDifferentOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Organisation A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Organisation B " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisationA, "john.cross.link." + UUID.randomUUID() + "@example.com", "John", "Doe"));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        assertThatThrownBy(() -> userService.linkEmployee(
                organisationA.getId(),
                user.getId(),
                employee.getId())).isInstanceOf(ResourceNotFoundException.class).hasMessage("Employee not found: " + employee.getId());

        User persisted = userRepository.findById(user.getId()).orElseThrow();

        assertThat(persisted.getEmployee()).isNull();
    }

    @Test
    void shouldRejectLinkingEmployeeAlreadyLinkedToAnotherUser() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Duplicate Employee Link Organisation " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User firstUser = new User(organisation, "first." + UUID.randomUUID() + "@example.com", "Jane", "Doe");

        firstUser.linkEmployee(employee);

        userRepository.saveAndFlush(firstUser);

        User secondUser = userRepository.saveAndFlush(new User(organisation, "second." + UUID.randomUUID() + "@example.com", "John", "Doe"));

        assertThatThrownBy(() -> userService.linkEmployee(organisation.getId(), secondUser.getId(), employee.getId())).isInstanceOf(IllegalStateException.class).hasMessage("Employee is already linked to another user");

        User persisted = userRepository.findById(secondUser.getId()).orElseThrow();

        assertThat(persisted.getEmployee()).isNull();
    }

    @Test
    void shouldRejectLinkingUserToDifferentEmployeeWhenAlreadyLinked() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Existing Employee Link Organisation " + UUID.randomUUID()));

        Employee firstEmployee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        Employee secondEmployee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        User user = new User(organisation, "linked." + UUID.randomUUID() + "@example.com", "Jane", "Doe");

        user.linkEmployee(firstEmployee);

        user = userRepository.saveAndFlush(user);

        UUID userId = user.getId();

        assertThatThrownBy(() -> userService.linkEmployee(organisation.getId(), userId, secondEmployee.getId())).isInstanceOf(IllegalStateException.class).hasMessage("User is already linked to an employee");

        User persisted = userRepository.findById(userId).orElseThrow();

        assertThat(persisted.getEmployee().getId()).isEqualTo(firstEmployee.getId());
    }

    @Test
    void shouldAllowLinkingSameUserToSameEmployeeAgain() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Idempotent Employee Link Organisation " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = new User(organisation, "same.link." + UUID.randomUUID() + "@example.com", "Jane", "Doe");

        user.linkEmployee(employee);

        user = userRepository.saveAndFlush(user);

        User linkedAgain = userService.linkEmployee(organisation.getId(), user.getId(), employee.getId());

        assertThat(linkedAgain.getEmployee()).isNotNull();

        assertThat(linkedAgain.getEmployee().getId()).isEqualTo(employee.getId());
    }

    @Test
    void shouldProvisionEmployeeUserAccount() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Employee Provisioning Organisation " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("jane.provision." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        User user = userService.provisionEmployeeAccount(organisation.getId(), employee.getId());

        assertThat(user.getId()).isNotNull();

        assertThat(user.getOrganisation().getId()).isEqualTo(organisation.getId());

        assertThat(user.getEmployee()).isNotNull();

        assertThat(user.getEmployee().getId()).isEqualTo(employee.getId());

        assertThat(user.getEmail()).isEqualTo(employee.getEmail());

        assertThat(user.getFirstName()).isEqualTo(employee.getFirstName());

        assertThat(user.getLastName()).isEqualTo(employee.getLastName());

        assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);

        assertThat(user.getPasswordHash()).isNull();

        assertThat(user.getRoles()).extracting(Role::getName).containsExactly("EMPLOYEE");
    }

    @Test
    void shouldRejectProvisioningEmployeeWhoAlreadyHasUserAccount() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Duplicate Employee Provisioning " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("jane.duplicate." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        userService.provisionEmployeeAccount(organisation.getId(), employee.getId());

        UUID employeeId = employee.getId();

        assertThatThrownBy(() -> userService.provisionEmployeeAccount(organisation.getId(), employeeId))
                .isInstanceOf(DuplicateResourceException.class).hasMessage("Employee already has a user account");
    }

    @Test
    void shouldRejectProvisioningWhenEmployeeEmailAlreadyBelongsToAnotherUser() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Duplicate Provisioning Email " + UUID.randomUUID()));

        String email = "duplicate.email." + UUID.randomUUID() + "@example.com";

        User existingUser = new User(organisation, email, "Existing", "User");

        userRepository.saveAndFlush(existingUser);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail(email);

        employee = employeeRepository.saveAndFlush(employee);

        UUID employeeId = employee.getId();

        assertThatThrownBy(() -> userService.provisionEmployeeAccount(organisation.getId(), employeeId))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User email already exists: " + email);
    }

    @Test
    void shouldRejectProvisioningEmployeeWithoutEmail() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Provision Employee Without Email " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID employeeId = employee.getId();

        assertThatThrownBy(() -> userService.provisionEmployeeAccount(organisation.getId(), employeeId))
                .isInstanceOf(InvalidUserProvisioningException.class)
                .hasMessage("Employee must have an email address before provisioning a user account");
    }

    @Test
    void shouldRejectProvisioningEmployeeFromAnotherOrganisation() {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Provisioning Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Provisioning Tenant B " + UUID.randomUUID()));

        Employee employee = new Employee(organisationB, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("cross.tenant." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        UUID employeeId = employee.getId();

        assertThatThrownBy(() -> userService.provisionEmployeeAccount(organisationA.getId(), employeeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee not found: " + employeeId);
    }

    @Test
    void shouldRejectProvisioningInactiveEmployee() {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Inactive Employee Provisioning " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("inactive." + UUID.randomUUID() + "@example.com");

        employee.setStatus(EmployeeStatus.INACTIVE);

        employee = employeeRepository.saveAndFlush(employee);

        UUID employeeId = employee.getId();

        assertThatThrownBy(() -> userService.provisionEmployeeAccount(organisation.getId(), employeeId))
                .isInstanceOf(InvalidUserProvisioningException.class)
                .hasMessage("Only active employees can be provisioned with a user account");
    }
}