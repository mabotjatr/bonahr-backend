package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    private String createAccessToken(UUID userId, UUID organisationId, List<String> roles) {
        return jwtService.generateToken(userId, organisationId, roles);
    }

    @Test
    void shouldAllowHrAdminToLinkUserToEmployee() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("User Linking API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = userRepository.saveAndFlush(new User(organisation, "jane.link." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeRoleLinksUserToEmployee() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Employee Role User Linking " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = userRepository.saveAndFlush(new User(organisation, "employee.role." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOwnerToLinkUserToEmployee() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Owner User Linking " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = userRepository.saveAndFlush(new User(organisation, "owner.link." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("OWNER"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()));
    }

    @Test
    void shouldNotLinkUserToEmployeeFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("User Linking Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("User Linking Tenant B " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisationA, "tenant.a." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        String token = createAccessToken(UUID.randomUUID(), organisationA.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotLinkEmployeeToUserFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("User Target Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("User Target Tenant B " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisationB, "tenant.b.user." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        String token = createAccessToken(UUID.randomUUID(), organisationA.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorizedWhenLinkingUserToEmployeeWithoutToken() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Unauthenticated User Linking " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        User user = userRepository.saveAndFlush(new User(organisation, "unauthenticated." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        String request = """
                {
                    "employeeId": "%s"
                }
                """.formatted(employee.getId());

        mockMvc.perform(patch("/api/v1/users/" + user.getId() + "/employee")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isUnauthorized());
    }
}