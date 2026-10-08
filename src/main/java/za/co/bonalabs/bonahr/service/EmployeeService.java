package za.co.bonalabs.bonahr.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.dto.employee.UpdateEmployeeRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.InvalidEmployeeStatusTransitionException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final OrganisationRepository organisationRepository;

    public EmployeeService(EmployeeRepository employeeRepository, OrganisationRepository organisationRepository) {
        this.employeeRepository = employeeRepository;
        this.organisationRepository = organisationRepository;
    }

    public Employee createEmployee(UUID organisationId, CreateEmployeeRequest request) {

        var organisation = organisationRepository.findById(organisationId).orElseThrow(() -> new ResourceNotFoundException("Organisation not found: " + organisationId));

        if (employeeRepository.existsByOrganisationIdAndEmployeeNumber(organisationId, request.employeeNumber())) {
            throw new DuplicateResourceException("Employee number already exists: " + request.employeeNumber());
        }

        if (request.email() != null && !request.email().isBlank() && employeeRepository.existsByOrganisationIdAndEmailIgnoreCase(organisationId, request.email())) {

            throw new DuplicateResourceException("Employee email already exists: " + request.email());
        }

        Employee employee = new Employee(organisation, request.employeeNumber(), request.firstName(), request.lastName());


        employee.setEmail(request.email());
        employee.setJobTitle(request.jobTitle());
        employee.setDepartment(request.department());
        employee.setEmploymentType(request.employmentType());
        employee.setStartDate(request.startDate());
        employee.setPhone(request.phone());

        return employeeRepository.saveAndFlush(employee);
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(UUID organisationId, UUID employeeId) {

        return employeeRepository.findByIdAndOrganisationId(employeeId, organisationId).orElseThrow(() ->
                new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployees(UUID organisationId) {
        return employeeRepository
                .findAllByOrganisationIdOrderByLastNameAscFirstNameAsc( organisationId);
    }

    public Employee updateEmployee(UUID organisationId, UUID employeeId, UpdateEmployeeRequest request) {

        Employee employee = getEmployee(organisationId, employeeId);

        if (request.email() != null && !request.email().isBlank()
                && !request.email().equalsIgnoreCase(employee.getEmail())
                && employeeRepository.existsByOrganisationIdAndEmailIgnoreCase(organisationId, request.email())) {

            throw new DuplicateResourceException("Employee email already exists: " + request.email());
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setJobTitle(request.jobTitle());
        employee.setDepartment(request.department());
        employee.setEmploymentType(request.employmentType());
        employee.setStartDate(request.startDate());
        employee.setPhone(request.phone());

        return employeeRepository.saveAndFlush(employee);
    }

    public Employee updateEmployeeStatus(UUID organisationId, UUID employeeId, EmployeeStatus status) {

        Employee employee = getEmployee(organisationId, employeeId);

        if (status == EmployeeStatus.TERMINATED) {
            throw new InvalidEmployeeStatusTransitionException("Employee termination must use the termination workflow");
        }

        employee.setStatus(status);

        return employeeRepository.saveAndFlush(employee);
    }

    @Transactional(readOnly = true)
    public List<Employee> searchEmployees(UUID organisationId, String search) {
        return employeeRepository
                .findAllByOrganisationIdAndFirstNameContainingIgnoreCaseOrOrganisationIdAndLastNameContainingIgnoreCaseOrderByLastNameAscFirstNameAsc(
                        organisationId,
                        search,
                        organisationId,
                        search
                );
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByStatus(UUID organisationId, EmployeeStatus status) {
        return employeeRepository
                .findAllByOrganisationIdAndStatusOrderByLastNameAscFirstNameAsc(
                        organisationId,
                        status
                );
    }

    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByDepartment(UUID organisationId, String department) {
        return employeeRepository.findAllByOrganisationIdAndDepartmentIgnoreCaseOrderByLastNameAscFirstNameAsc(organisationId, department);
    }

    @Transactional(readOnly = true)
    public List<Employee> filterEmployees(UUID organisationId, String search, EmployeeStatus status, String department) {

        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim().toLowerCase(Locale.ROOT);

        String normalizedDepartment = department == null || department.isBlank() ? "" : department.trim().toLowerCase(Locale.ROOT);

        return employeeRepository.findAllByFilters(organisationId, normalizedSearch, status, normalizedDepartment);
    }

    @Transactional(readOnly = true)
    public Page<Employee> filterEmployeesPaged(UUID organisationId, String search, EmployeeStatus status, String department, Pageable pageable) {

        String normalizedSearch = search == null || search.isBlank() ? "" : search.trim().toLowerCase(Locale.ROOT);

        String normalizedDepartment = department == null || department.isBlank() ? "" : department.trim().toLowerCase(Locale.ROOT);

        return employeeRepository.findAllByFiltersPaged(organisationId, normalizedSearch, status, normalizedDepartment, pageable);
    }
}