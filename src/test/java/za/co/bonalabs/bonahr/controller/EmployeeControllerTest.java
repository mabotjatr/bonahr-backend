package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.security.JwtService;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(UUID.randomUUID(), organisationId, roles);
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
}