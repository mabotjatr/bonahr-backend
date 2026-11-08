package za.co.bonalabs.bonahr.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.InvalidUserProvisioningException;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.RoleRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final UserInvitationService userInvitationService;
    private final InvitationDeliveryService invitationDeliveryService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository,
            UserInvitationService userInvitationService,
            InvitationDeliveryService invitationDeliveryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.userInvitationService = userInvitationService;
        this.invitationDeliveryService = invitationDeliveryService;
    }

    @Transactional
    public User setPassword(UUID userId, String rawPassword) {

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        return user;
    }

    @Transactional(readOnly = true)
    public Employee resolveEmployee(UUID organisationId, UUID userId) {

        User user = userRepository.findByIdAndOrganisationId(userId, organisationId).orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Employee employee = user.getEmployee();

        if (employee == null) {
            throw new IllegalStateException("User is not linked to an employee");
        }

        return employee;
    }

    @Transactional
    public User linkEmployee(UUID organisationId, UUID userId, UUID employeeId) {

        User user = userRepository.findByIdAndOrganisationId(userId, organisationId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Employee employee = employeeRepository.findByIdAndOrganisationId(
                employeeId,
                organisationId).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        Employee existingEmployee = user.getEmployee();

        if (existingEmployee != null && !existingEmployee.getId().equals(employeeId)) {
            throw new IllegalStateException(
                    "User is already linked to an employee"
            );
        }

        userRepository.findByEmployeeId(employeeId)
                .filter(linkedUser -> !linkedUser.getId().equals(userId))
                .ifPresent(linkedUser -> {
                    throw new IllegalStateException(
                            "Employee is already linked to another user"
                    );
                });

        user.linkEmployee(employee);

        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User provisionEmployeeAccount(UUID organisationId, UUID employeeId) {

        Employee employee = employeeRepository.findByIdAndOrganisationId( employeeId,organisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new InvalidUserProvisioningException("Only active employees can be provisioned with a user account");
        }

        userRepository.findByEmployeeId(employeeId)
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("Employee already has a user account");
                });

        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            throw new InvalidUserProvisioningException("Employee must have an email address before provisioning a user account");
        }

        if (userRepository.existsByOrganisationIdAndEmail(organisationId, employee.getEmail())) {
            throw new DuplicateResourceException("User email already exists: " + employee.getEmail());
        }

        Role employeeRole = roleRepository.findByName("EMPLOYEE").orElseThrow(() -> new IllegalStateException("EMPLOYEE role is not configured"));

        User user = new User(employee.getOrganisation(), employee.getEmail(), employee.getFirstName(), employee.getLastName());

        user.addRole(employeeRole);

        user.linkEmployee(employee);

        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public ProvisionedEmployeeAccount provisionEmployeeAccountWithInvitation(UUID organisationId, UUID employeeId) {

        User user = provisionEmployeeAccount(organisationId, employeeId);

        UserInvitationService.CreatedInvitation invitation = userInvitationService.createInvitation(organisationId, user.getId());

        invitationDeliveryService.sendSetupInvitation(user, invitation.rawToken());

        return new ProvisionedEmployeeAccount(user, invitation);
    }

    public record ProvisionedEmployeeAccount(
            User user,
            UserInvitationService.CreatedInvitation invitation
    ) {
    }
}