package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.dto.employee.UpdateEmployeeRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeServiceTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldCreateEmployeeForOrganisation() {

        Organisation organisation = new Organisation("Employee Service Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        CreateEmployeeRequest request = new CreateEmployeeRequest("EMP-" + uniqueValue, "John", "Doe", "john-" + uniqueValue + "@example.com");

        Employee employee = employeeService.createEmployee(organisation.getId(), request);

        assertNotNull(employee.getId());

        assertEquals(organisation.getId(), employee.getOrganisation().getId());

        assertEquals(request.employeeNumber(), employee.getEmployeeNumber());

        assertEquals("John", employee.getFirstName());

        assertEquals("Doe", employee.getLastName());

        assertEquals(request.email(), employee.getEmail());

        assertEquals(EmployeeStatus.ACTIVE, employee.getStatus());
    }

    @Test
    void shouldRejectEmployeeCreationForUnknownOrganisation() {

        UUID unknownOrganisationId = UUID.randomUUID();

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Jane", "Doe", "jane-" + UUID.randomUUID() + "@example.com");

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(unknownOrganisationId, request));

        assertEquals("Organisation not found: " + unknownOrganisationId, exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmployeeNumberWithinSameOrganisation() {

        Organisation organisation = new Organisation("Duplicate Employee Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String employeeNumber = "EMP-001";

        CreateEmployeeRequest firstRequest = new CreateEmployeeRequest(
                employeeNumber, "John", "Doe", "john-" + UUID.randomUUID() + "@example.com");

        employeeService.createEmployee(organisation.getId(), firstRequest);

        CreateEmployeeRequest secondRequest = new CreateEmployeeRequest(
                employeeNumber, "Jane", "Doe", "jane-" + UUID.randomUUID() + "@example.com");

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(finalOrganisation.getId(), secondRequest));
    }

    @Test
    void shouldRejectDuplicateEmployeeEmailWithinSameOrganisation() {

        Organisation organisation = new Organisation("Duplicate Email Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String email = "employee-" + UUID.randomUUID() + "@example.com";

        CreateEmployeeRequest firstRequest = new CreateEmployeeRequest("EMP-001", "John", "Doe", email);

        employeeService.createEmployee(organisation.getId(), firstRequest);

        CreateEmployeeRequest secondRequest = new CreateEmployeeRequest("EMP-002", "Jane", "Doe", email.toUpperCase());

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(finalOrganisation.getId(), secondRequest));
    }

    @Test
    void shouldAllowSameEmployeeNumberAndEmailAcrossDifferentOrganisations() {

        Organisation organisationA = new Organisation("Organisation A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Organisation B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        String employeeNumber = "EMP-001";
        String email = "shared.employee@example.com";

        CreateEmployeeRequest requestA = new CreateEmployeeRequest(employeeNumber, "John", "Doe", email);

        CreateEmployeeRequest requestB = new CreateEmployeeRequest(employeeNumber, "Jane", "Doe", email);

        Employee employeeA = employeeService.createEmployee(organisationA.getId(), requestA);

        Employee employeeB = employeeService.createEmployee(organisationB.getId(), requestB);

        assertNotNull(employeeA.getId());
        assertNotNull(employeeB.getId());

        assertNotEquals(employeeA.getId(), employeeB.getId());

        assertEquals(organisationA.getId(), employeeA.getOrganisation().getId());

        assertEquals(organisationB.getId(), employeeB.getOrganisation().getId());
    }

    @Test
    void shouldReturnEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Lookup Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Lookup", "Employee", "lookup-" + UUID.randomUUID() + "@example.com"
        );

        Employee createdEmployee = employeeService.createEmployee(organisation.getId(), request);

        Employee retrievedEmployee = employeeService.getEmployee(organisation.getId(), createdEmployee.getId());

        assertEquals(createdEmployee.getId(), retrievedEmployee.getId());

        assertEquals(organisation.getId(), retrievedEmployee.getOrganisation().getId());

        assertEquals("Lookup", retrievedEmployee.getFirstName());
    }

    @Test
    void shouldNotReturnEmployeeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Tenant", "Employee", "tenant-" + UUID.randomUUID() + "@example.com"
        );

        Employee employee = employeeService.createEmployee(organisationA.getId(), request);

        Organisation finalOrganisationB = organisationB;
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.getEmployee(finalOrganisationB.getId(), employee.getId()));

        assertEquals("Employee not found: " + employee.getId(), exception.getMessage());
    }

    @Test
    void shouldReturnOnlyEmployeesForOrganisation() {

        Organisation organisationA = new Organisation("Employee List A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee List B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-A-001", "John", "Zulu", "john-a-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-A-002", "Jane", "Adams", "jane-a-" + UUID.randomUUID() + "@example.com"));

        employeeService.createEmployee(organisationB.getId(), new CreateEmployeeRequest(
                "EMP-B-001", "Other", "Tenant", "other-" + UUID.randomUUID() + "@example.com"));

        List<Employee> employees = employeeService.getEmployees(organisationA.getId());

        assertEquals(2, employees.size());

        Organisation finalOrganisationA = organisationA;
        assertTrue(employees.stream().allMatch(employee -> employee.getOrganisation().getId().equals(finalOrganisationA.getId())));

        assertEquals("Adams", employees.get(0).getLastName());

        assertEquals("Zulu", employees.get(1).getLastName());
    }

    @Test
    void shouldUpdateEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Update Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        CreateEmployeeRequest createRequest = new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "John", "Doe", "john-" + UUID.randomUUID() + "@example.com");

        Employee employee = employeeService.createEmployee(organisation.getId(), createRequest);

        String updatedEmail = "updated-" + UUID.randomUUID() + "@example.com";

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest("Johnny", "Smith", updatedEmail);

        Employee updatedEmployee = employeeService.updateEmployee(organisation.getId(), employee.getId(), updateRequest);

        assertEquals(employee.getId(), updatedEmployee.getId());

        assertEquals(organisation.getId(), updatedEmployee.getOrganisation().getId());

        assertEquals("Johnny", updatedEmployee.getFirstName());

        assertEquals("Smith", updatedEmployee.getLastName());

        assertEquals(updatedEmail, updatedEmployee.getEmail());

        // Employee number must remain unchanged.
        assertEquals(createRequest.employeeNumber(), updatedEmployee.getEmployeeNumber());

        // Normal profile editing must not change employment status.
        assertEquals(EmployeeStatus.ACTIVE, updatedEmployee.getStatus());
    }

    @Test
    void shouldNotUpdateEmployeeFromAnotherOrganisation() {

        Organisation organisationA = new Organisation("Employee Update Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Update Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = employeeService.createEmployee(organisationA.getId(), new CreateEmployeeRequest(
                "EMP-" + UUID.randomUUID(), "Original", "Employee", "original-" + UUID.randomUUID() + "@example.com"));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "Changed", "Employee", "changed-" + UUID.randomUUID() + "@example.com");

        Organisation finalOrganisationB = organisationB;
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                employeeService.updateEmployee(finalOrganisationB.getId(), employee.getId(), updateRequest));

        assertEquals("Employee not found: " + employee.getId(), exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmployeeEmailDuringUpdate() {

        Organisation organisation = new Organisation("Employee Update Email Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String firstEmail = "first-" + UUID.randomUUID() + "@example.com";

        String secondEmail = "second-" + UUID.randomUUID() + "@example.com";

        employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-001-" + UUID.randomUUID(), "First", "Employee", firstEmail));

        Employee secondEmployee = employeeService.createEmployee(organisation.getId(), new CreateEmployeeRequest(
                "EMP-002-" + UUID.randomUUID(), "Second", "Employee", secondEmail));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(secondEmployee.getFirstName(), secondEmployee.getLastName(), firstEmail.toUpperCase());

        Organisation finalOrganisation = organisation;
        assertThrows(DuplicateResourceException.class, () -> employeeService.updateEmployee(finalOrganisation.getId(), secondEmployee.getId(), updateRequest));
    }
}