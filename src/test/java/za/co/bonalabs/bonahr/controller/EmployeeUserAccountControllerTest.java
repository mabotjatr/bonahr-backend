package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserInvitationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeUserAccountControllerTest {

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

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Test
    void shouldAllowHrAdminToProvisionEmployeeUserAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Employee Account Provisioning " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("jane.employee." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.email").value(employee.getEmail()))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));

        User provisionedUser = userRepository.findByEmployeeId(employee.getId())
                        .orElseThrow();

        List<UserInvitation> invitations = userInvitationRepository.findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(provisionedUser.getId());

        assertThat(invitations).hasSize(1);

        assertThat(invitations.getFirst().getTokenHash()).isNotBlank();
    }

    @Test
    void shouldAllowOwnerToProvisionEmployeeUserAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Owner Employee Provisioning " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("owner.provision." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("OWNER"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.email").value(employee.getEmail()))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeProvisionsUserAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Employee Provisioning Forbidden " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("forbidden.provision." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenProvisioningUserAccountWithoutToken() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Unauthenticated Employee Provisioning " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("unauthenticated.provision." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenProvisioningEmployeeFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Provisioning Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Provisioning Tenant B " + UUID.randomUUID()));

        Employee employee = new Employee(organisationB, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("cross.tenant.provision." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenEmployeeAlreadyHasUserAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Duplicate Employee Account " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("duplicate.account." + UUID.randomUUID() + "@example.com");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        String endpoint = "/api/v1/employees/" + employee.getId() + "/user-account";

        mockMvc.perform(post(endpoint).header("Authorization", "Bearer " + token)).andExpect(status().isCreated());

        mockMvc.perform(post(endpoint).header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
    }

    @Test
    void shouldReturnConflictWhenEmployeeEmailAlreadyBelongsToAnotherUser() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Duplicate Provisioning Email " + UUID.randomUUID()));

        String email = "duplicate.api." + UUID.randomUUID() + "@example.com";

        User existingUser = new User(organisation, email, "Existing", "User");

        userRepository.saveAndFlush(existingUser);

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail(email);

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account").header("Authorization", "Bearer " + token)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT")).andExpect(jsonPath("$.message").value("User email already exists: " + email));
    }

    @Test
    void shouldReturnBadRequestWhenEmployeeHasNoEmail() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Provisioning Without Email " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenEmployeeIsInactive() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Inactive Employee Provisioning API " + UUID.randomUUID()));

        Employee employee = new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe");

        employee.setEmail("inactive.api." + UUID.randomUUID() + "@example.com");

        employee.setStatus(EmployeeStatus.INACTIVE);

        employee = employeeRepository.saveAndFlush(employee);

        String token = createAccessToken(UUID.randomUUID(), organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(post("/api/v1/employees/" + employee.getId() + "/user-account")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_PROVISIONING"))
                .andExpect(jsonPath("$.message").value("Only active employees can be provisioned with a user account"));
    }

    private String createAccessToken(UUID userId, UUID organisationId, List<String> roles) {
        return jwtService.generateToken(userId, organisationId, roles);
    }
}