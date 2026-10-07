package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;
import za.co.bonalabs.bonahr.entity.Organisation;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldPersistAndFindEmployeeWithinOrganisation() {

        Organisation organisation = new Organisation("Employee Test Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        Employee employee = new Employee(organisation, "EMP-" + uniqueValue, "Test", "Employee");

        employee.setEmail("employee-" + uniqueValue + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        Employee persistedEmployee = employeeRepository.findByIdAndOrganisationId(employee.getId(), organisation.getId()).orElseThrow();

        assertNotNull(persistedEmployee.getId());

        assertEquals(organisation.getId(), persistedEmployee.getOrganisation().getId());

        assertEquals(employee.getEmployeeNumber(), persistedEmployee.getEmployeeNumber());

        assertEquals("Test", persistedEmployee.getFirstName());

        assertEquals("Employee", persistedEmployee.getLastName());

        assertEquals(EmployeeStatus.ACTIVE, persistedEmployee.getStatus());

        assertNotNull(persistedEmployee.getCreatedAt());
        assertNotNull(persistedEmployee.getUpdatedAt());
    }

    @Test
    void shouldNotFindEmployeeUsingAnotherOrganisationId() {

        Organisation organisationA = new Organisation("Organisation A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Organisation B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        String uniqueValue = UUID.randomUUID().toString();

        Employee employee = new Employee(organisationA, "EMP-" + uniqueValue, "Tenant", "Employee");

        employee = employeeRepository.saveAndFlush(employee);

        var result = employeeRepository.findByIdAndOrganisationId(employee.getId(), organisationB.getId());

        assertTrue(result.isEmpty());
    }
}