package za.co.bonalabs.bonahr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.controller.dto.LoginRequest;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.RoleRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.service.UserInvitationService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserInvitationService userInvitationService;

    private User activeUser;

    @BeforeEach
    void setUp() {

        Organisation organisation = organisationRepository.save(new Organisation("Test Organisation"));

        Role role = roleRepository.findByName("HR_ADMIN").orElseThrow();

        activeUser = new User(organisation, "john@example.com", "John", "Doe");

        activeUser.setPasswordHash(passwordEncoder.encode("SecretPassword123!"));

        activeUser.setStatus(UserStatus.ACTIVE);

        activeUser.addRole(role);

        activeUser = userRepository.save(activeUser);
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        LoginRequest request = new LoginRequest(activeUser.getOrganisation().getId(), "john@example.com", "SecretPassword123!");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.organisationId").value(activeUser.getOrganisation().getId().toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("HR_ADMIN"));
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {

        LoginRequest request = new LoginRequest(activeUser.getOrganisation().getId(), "john@example.com", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {

        LoginRequest request = new LoginRequest(activeUser.getOrganisation().getId(), "unknown@example.com", "SecretPassword123!");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBlankEmail() throws Exception {

        LoginRequest request = new LoginRequest(activeUser.getOrganisation().getId(), "", "SecretPassword123!");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {

        LoginRequest request = new LoginRequest(activeUser.getOrganisation().getId(), "john@example.com", "");

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSetupPasswordUsingValidInvitationToken() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Password Setup Organisation " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "setup.password." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation invitation = userInvitationService.createInvitation(organisation.getId(), user.getId());

        String request = """
                {
                    "token": "%s",
                    "password": "StrongPassword123!"
                }
                """.formatted(invitation.rawToken());

        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturnNotFoundWhenSetupPasswordTokenIsInvalid() throws Exception {

        String request = """
                {
                    "token": "invalid-invitation-token",
                    "password": "StrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Invitation not found"));
    }

    @Test
    void shouldReturnBadRequestWhenSetupPasswordTokenIsRevoked() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Revoked Setup Password " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "revoked.setup." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation first = userInvitationService.createInvitation(organisation.getId(), user.getId());

        // Reissuing revokes the first invitation.
        userInvitationService.createInvitation(organisation.getId(), user.getId());

        String request = """
                {
                    "token": "%s",
                    "password": "StrongPassword123!"
                }
                """.formatted(first.rawToken());

        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenSetupPasswordTokenWasAlreadyUsed() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Used Setup Password " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "used.setup." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation invitation = userInvitationService.createInvitation(organisation.getId(), user.getId());

        String request = """
                {
                    "token": "%s",
                    "password": "StrongPassword123!"
                }
                """.formatted(invitation.rawToken());

        // First use succeeds.
        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isOk());

        // Same token must never work again.
        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER_INVITATION"))
                .andExpect(jsonPath("$.message").value("Invitation is no longer usable"));
    }

    @Test
    void shouldRejectSetupPasswordWhenPasswordIsTooShort() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Weak Password Organisation " + UUID.randomUUID()));

        User user = userRepository.saveAndFlush(new User(organisation, "weak.password." + UUID.randomUUID() + "@example.com", "Jane", "Doe"));

        UserInvitationService.CreatedInvitation invitation = userInvitationService.createInvitation(organisation.getId(), user.getId());

        String request = """
                {
                    "token": "%s",
                    "password": "123456"
                }
                """.formatted(invitation.rawToken());

        mockMvc.perform(post("/api/v1/auth/setup-password").contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isBadRequest());
    }
}