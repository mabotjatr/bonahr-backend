package za.co.bonalabs.bonahr.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.repository.EmployeeAuditLogRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.security.JwtService;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeAuditLogRepository employeeAuditLogRepository;

    private String createAccessToken(UUID userId, UUID organisationId, List<String> roles) {
        return jwtService.generateToken(userId, organisationId, roles);
    }

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return createAccessToken(UUID.randomUUID(), organisationId, roles);
    }

    @Test
    void shouldCreateEmployeeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "employeeNumber": "EMP-%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john-%s@example.com"
                }
                """.formatted(uniqueValue, uniqueValue);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-" + uniqueValue))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john-" + uniqueValue + "@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingEmployeeWithoutToken() throws Exception {

        String request = """
                {
                    "employeeNumber": "EMP-001",
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/employees").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleCreatesEmployee() throws Exception {

        Organisation organisation = new Organisation("Employee Role Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "employeeNumber": "EMP-%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john-%s@example.com"
                }
                """.formatted(uniqueValue, uniqueValue);

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(post("/api/v1/employees").header(
                "Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden()
        );
    }

    @Test
    void shouldAllowOwnerToCreateEmployee() throws Exception {

        Organisation organisation = new Organisation("Owner Employee Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "employeeNumber": "EMP-%s",
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "jane-%s@example.com"
                }
                """.formatted(uniqueValue, uniqueValue);

        String token = createAccessToken(organisation.getId(), List.of("OWNER"));

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-" + uniqueValue))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnEmployeeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Lookup API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        Employee employee = new Employee(organisation, "EMP-" + uniqueValue, "Lookup", "Employee");

        employee.setEmail("lookup-" + uniqueValue + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees/" + employee.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employee.getId().toString()))
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.employeeNumber").value(employee.getEmployeeNumber()))
                .andExpect(jsonPath("$.firstName").value("Lookup"))
                .andExpect(jsonPath("$.lastName").value("Employee"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldNotReturnEmployeeFromAnotherOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Employee Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Tenant", "Employee");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(organisationB.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees/" + employee.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleReadsEmployee() throws Exception {

        Organisation organisation = new Organisation("Employee Read Role Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Read", "Employee");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/employees/" + employee.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOwnerToReadEmployee() throws Exception {

        Organisation organisation = new Organisation("Owner Read Employee Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Owner", "Visible");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(organisation.getId(), List.of("OWNER"));

        mockMvc.perform(get("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(employee.getId().toString()))
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Owner"))
                .andExpect(jsonPath("$.lastName").value("Visible"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnOnlyEmployeesForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Employee List API A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee List API B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employeeA1 = new Employee(organisationA, "EMP-A-001", "John", "Zulu");

        Employee employeeA2 = new Employee(organisationA, "EMP-A-002", "Jane", "Adams");

        Employee employeeB = new Employee(organisationB, "EMP-B-001", "Other", "Tenant");

        employeeRepository.saveAndFlush(employeeA1);
        employeeRepository.saveAndFlush(employeeA2);
        employeeRepository.saveAndFlush(employeeB);

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organisationId").value(organisationA.getId().toString()))
                .andExpect(jsonPath("$[1].organisationId").value(organisationA.getId().toString()))
                .andExpect(jsonPath("$[0].lastName").value("Adams"))
                .andExpect(jsonPath("$[1].lastName").value("Zulu"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleListsEmployees() throws Exception {

        Organisation organisation = new Organisation("Employee List Role Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/employees").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateEmployeeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Update API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee.setEmail("john-" + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String updatedEmail = "updated-" + UUID.randomUUID() + "@example.com";

        String request = """
                {
                    "firstName": "Johnny",
                    "lastName": "Smith",
                    "email": "%s"
                }
                """.formatted(updatedEmail);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(employee.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value(updatedEmail))
                .andExpect(jsonPath("$.employeeNumber").value(employee.getEmployeeNumber()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleUpdatesEmployee() throws Exception {

        Organisation organisation = new Organisation("Employee Update Role Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "firstName": "Johnny",
                    "lastName": "Smith",
                    "email": "johnny@example.com"
                }
                """;

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotUpdateEmployeeFromAnotherOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Employee Update Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Employee Update Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        Employee employee = new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Original", "Employee");

        employee.setEmail("original-" + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "firstName": "Changed",
                    "lastName": "Employee",
                    "email": "changed@example.com"
                }
                """;

        String token = createAccessToken(organisationB.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenUpdatingEmployeeToDuplicateEmail() throws Exception {

        Organisation organisation = new Organisation("Employee Duplicate Update Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String firstEmail = "first-" + UUID.randomUUID() + "@example.com";

        String secondEmail = "second-" + UUID.randomUUID() + "@example.com";

        Employee firstEmployee = new Employee(organisation, "EMP-001-" + UUID.randomUUID(), "First", "Employee");

        firstEmployee.setEmail(firstEmail);

        employeeRepository.saveAndFlush(firstEmployee);

        Employee secondEmployee = new Employee(organisation, "EMP-002-" + UUID.randomUUID(), "Second", "Employee");

        secondEmployee.setEmail(secondEmail);

        secondEmployee = employeeRepository.saveAndFlush(secondEmployee);

        String request = """
                {
                    "firstName": "Second",
                    "lastName": "Employee",
                    "email": "%s"
                }
                """.formatted(firstEmail.toUpperCase());

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(put("/api/v1/employees/" + secondEmployee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeactivateEmployeeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Status API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "status": "INACTIVE"
                }
                """;

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(employee.getId().toString())).andExpect(jsonPath("$.organisationId").value(organisation.getId().toString())).andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleUpdatesEmployeeStatus() throws Exception {

        Organisation organisation = new Organisation("Employee Status Role Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "status": "INACTIVE"
                }
                """;

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectTerminationThroughStatusEndpoint() throws Exception {

        Organisation organisation = new Organisation("Employee Termination API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "status": "TERMINATED"
                }
                """;

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUpdatedEmploymentDetails() throws Exception {

        Organisation organisation = new Organisation("Employment Details API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee.setEmail("john-" + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String request = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "%s",
                    "jobTitle": "Senior Software Engineer",
                    "department": "Engineering",
                    "employmentType": "PERMANENT",
                    "startDate": "2026-10-01",
                    "phone": "+27 82 123 4567"
                }
                """.formatted(employee.getEmail());

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.jobTitle").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.employmentType").value("PERMANENT"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.phone").value("+27 82 123 4567"));
    }

    @Test
    void shouldReturnEmploymentDetailsWhenRetrievingEmployee() throws Exception {

        Organisation organisation = new Organisation("Employment Read API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee.setEmail("john-" + UUID.randomUUID() + "@example.com");

        employee.setJobTitle("Senior Software Engineer");
        employee.setDepartment("Engineering");
        employee.setEmploymentType(EmploymentType.PERMANENT);
        employee.setStartDate(LocalDate.of(2026, 10, 1));
        employee.setPhone("+27 82 123 4567");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Senior Software Engineer"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.employmentType").value("PERMANENT"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.phone").value("+27 82 123 4567"));

    }

    @Test
    void shouldCreateEmployeeWithEmploymentDetails() throws Exception {

        Organisation organisation = new Organisation("Employee Create Details Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "employeeNumber": "EMP-%s",
                    "firstName": "Jane",
                    "lastName": "Doe",
                    "email": "jane-%s@example.com",
                    "jobTitle": "HR Manager",
                    "department": "Human Resources",
                    "employmentType": "PERMANENT",
                    "startDate": "2026-10-01",
                    "phone": "+27 82 987 6543"
                }
                """.formatted(uniqueValue, uniqueValue);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.jobTitle").value("HR Manager"))
                .andExpect(jsonPath("$.department").value("Human Resources"))
                .andExpect(jsonPath("$.employmentType").value("PERMANENT"))
                .andExpect(jsonPath("$.startDate").value("2026-10-01"))
                .andExpect(jsonPath("$.phone").value("+27 82 987 6543"));
    }

    @Test
    void shouldSearchEmployeesByNameForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Search API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee john = new Employee(organisation, "EMP-001-" + UUID.randomUUID(), "John", "Smith");

        Employee jane = new Employee(organisation, "EMP-002-" + UUID.randomUUID(), "Jane", "Doe");

        employeeRepository.saveAndFlush(john);
        employeeRepository.saveAndFlush(jane);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees").param("search", "john")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Smith"));
    }

    @Test
    void shouldFilterEmployeesByStatusForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Status Filter API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee activeEmployee = new Employee(organisation, "EMP-A-" + UUID.randomUUID(), "Active", "Employee");

        Employee inactiveEmployee = new Employee(organisation, "EMP-I-" + UUID.randomUUID(), "Inactive", "Employee");

        activeEmployee = employeeRepository.saveAndFlush(activeEmployee);

        inactiveEmployee = employeeRepository.saveAndFlush(inactiveEmployee);

        inactiveEmployee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.saveAndFlush(inactiveEmployee);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees").param("status", "ACTIVE")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activeEmployee.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldFilterEmployeesByDepartmentForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Department Filter API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee engineeringEmployee = new Employee(organisation, "EMP-E-" + UUID.randomUUID(), "John", "Engineer");

        engineeringEmployee.setDepartment("Engineering");

        Employee hrEmployee = new Employee(organisation, "EMP-H-" + UUID.randomUUID(), "Jane", "HR");

        hrEmployee.setDepartment("Human Resources");

        engineeringEmployee = employeeRepository.saveAndFlush(engineeringEmployee);

        employeeRepository.saveAndFlush(hrEmployee);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees").param("department", "engineering")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(engineeringEmployee.getId().toString()))
                .andExpect(jsonPath("$[0].department").value("Engineering"));
    }

    @Test
    void shouldCombineEmployeeFilters() throws Exception {

        Organisation organisation = new Organisation("Combined Filter Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        // Should match all filters
        Employee matchingEmployee = new Employee(organisation, "EMP-001-" + UUID.randomUUID(), "John", "Smith");

        matchingEmployee.setDepartment("Engineering");

        matchingEmployee = employeeRepository.saveAndFlush(matchingEmployee);

        // Matches name + department, but wrong status
        Employee inactiveEmployee = new Employee(organisation, "EMP-002-" + UUID.randomUUID(), "John", "Doe");

        inactiveEmployee.setDepartment("Engineering");
        inactiveEmployee.setStatus(EmployeeStatus.INACTIVE);

        employeeRepository.saveAndFlush(inactiveEmployee);

        // Matches name + status, but wrong department
        Employee hrEmployee = new Employee(organisation, "EMP-003-" + UUID.randomUUID(), "John", "Jones");

        hrEmployee.setDepartment("Human Resources");

        employeeRepository.saveAndFlush(hrEmployee);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees")
                .param("search", "john")
                .param("status", "ACTIVE")
                .param("department", "Engineering")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(matchingEmployee.getId().toString()))
                .andExpect(jsonPath("$[0].department").value("Engineering"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturnPaginatedEmployeesForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Employee Pagination API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        for (int i = 1; i <= 5; i++) {

            Employee employee = new Employee(organisation, "EMP-" + i + "-" + UUID.randomUUID(), "Employee" + i, "User" + i);

            employeeRepository.saveAndFlush(employee);
        }

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees/paged")
                .param("page", "0")
                .param("size", "2")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void shouldReturnFilteredPaginatedEmployeesForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Paged Filter Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Paged Filter Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        // Matches all filters
        Employee matchingEmployee = new Employee(organisationA, "EMP-A-001-" + UUID.randomUUID(), "John", "Smith");

        matchingEmployee.setDepartment("Engineering");

        matchingEmployee = employeeRepository.saveAndFlush(matchingEmployee);

        // Same tenant, but wrong status
        Employee inactiveEmployee = new Employee(organisationA, "EMP-A-002-" + UUID.randomUUID(), "John", "Doe");

        inactiveEmployee.setDepartment("Engineering");
        inactiveEmployee.setStatus(EmployeeStatus.INACTIVE);

        employeeRepository.saveAndFlush(inactiveEmployee);

        // Same tenant, wrong department
        Employee hrEmployee = new Employee(organisationA, "EMP-A-003-" + UUID.randomUUID(), "John", "Jones");

        hrEmployee.setDepartment("Human Resources");

        employeeRepository.saveAndFlush(hrEmployee);

        // Different tenant — must never appear
        Employee otherTenantEmployee = new Employee(organisationB, "EMP-B-001-" + UUID.randomUUID(), "John", "OtherTenant");

        otherTenantEmployee.setDepartment("Engineering");

        employeeRepository.saveAndFlush(otherTenantEmployee);

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/employees/paged")
                .param("page", "0")
                .param("size", "2")
                .param("search", "john")
                .param("status", "ACTIVE")
                .param("department", "Engineering")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content[0].id").value(matchingEmployee.getId().toString()))
                .andExpect(jsonPath("$.content[0].department").value("Engineering"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void shouldRecordAuthenticatedUserWhenEmployeeIsUpdated() throws Exception {

        Organisation organisation = new Organisation("Employee Audit API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee.setEmail("john-" + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        UUID actorUserId = UUID.randomUUID();

        String token = createAccessToken(actorUserId, organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "firstName": "Johnny",
                    "lastName": "Doe",
                    "email": "%s"
                }
                """.formatted(employee.getEmail());

        mockMvc.perform(put("/api/v1/employees/" + employee.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.EMPLOYEE_UPDATED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());
    }

    @Test
    void shouldRecordAuthenticatedUserWhenEmployeeStatusChanges() throws Exception {

        Organisation organisation = new Organisation("Employee Status Audit API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "John", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        UUID actorUserId = UUID.randomUUID();

        String token = createAccessToken(actorUserId, organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(patch("/api/v1/employees/" + employee.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk());

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employee.getId());

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.STATUS_CHANGED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());

        @SuppressWarnings("unchecked")
        Map<String, Object> statusChange = (Map<String, Object>) auditLog.getChanges().get("status");

        assertEquals("ACTIVE", statusChange.get("from"));

        assertEquals("INACTIVE", statusChange.get("to"));
    }

    @Test
    void shouldRecordAuthenticatedUserWhenEmployeeIsCreated() throws Exception {

        Organisation organisation = new Organisation("Employee Creation Audit API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        UUID actorUserId = UUID.randomUUID();

        String token = createAccessToken(actorUserId, organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeNumber": "EMP-%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john-%s@example.com"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseBody);

        UUID employeeId = UUID.fromString(responseJson.get("id").asText());

        List<EmployeeAuditLog> auditLogs = employeeAuditLogRepository.findAllByOrganisationIdAndEmployeeIdOrderByCreatedAtDesc(organisation.getId(), employeeId);

        assertEquals(1, auditLogs.size());

        EmployeeAuditLog auditLog = auditLogs.getFirst();

        assertEquals(EmployeeAuditAction.EMPLOYEE_CREATED, auditLog.getAction());

        assertEquals(actorUserId, auditLog.getActorUserId());
    }
}