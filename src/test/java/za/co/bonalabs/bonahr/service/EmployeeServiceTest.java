package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.employee.CreateEmployeeRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;

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
}