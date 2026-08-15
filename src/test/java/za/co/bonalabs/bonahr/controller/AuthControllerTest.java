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

    private User activeUser;

    @BeforeEach
    void setUp() {

        Organisation organisation =
                organisationRepository.save(
                        new Organisation("Test Organisation")
                );

        Role role =
                roleRepository.findByName("HR_ADMIN")
                        .orElseThrow();

        activeUser = new User(
                organisation,
                "john@example.com",
                "John",
                "Doe"
        );

        activeUser.setPasswordHash(
                passwordEncoder.encode("SecretPassword123!")
        );

        activeUser.setStatus(UserStatus.ACTIVE);

        activeUser.addRole(role);

        activeUser =
                userRepository.save(activeUser);
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        LoginRequest request = new LoginRequest(
                activeUser.getOrganisation().getId(),
                "john@example.com",
                "SecretPassword123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.organisationId")
                        .value(activeUser.getOrganisation().getId().toString()))
                .andExpect(jsonPath("$.email")
                        .value("john@example.com"))
                .andExpect(jsonPath("$.roles[0]")
                        .value("HR_ADMIN"));
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {

        LoginRequest request = new LoginRequest(
                activeUser.getOrganisation().getId(),
                "john@example.com",
                "WrongPassword123!"
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnknownUser() throws Exception {

        LoginRequest request = new LoginRequest(
                activeUser.getOrganisation().getId(),
                "unknown@example.com",
                "SecretPassword123!"
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBlankEmail() throws Exception {

        LoginRequest request = new LoginRequest(
                activeUser.getOrganisation().getId(),
                "",
                "SecretPassword123!"
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {

        LoginRequest request = new LoginRequest(
                activeUser.getOrganisation().getId(),
                "john@example.com",
                ""
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }
}