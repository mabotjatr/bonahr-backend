package za.co.bonalabs.bonahr.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
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
}