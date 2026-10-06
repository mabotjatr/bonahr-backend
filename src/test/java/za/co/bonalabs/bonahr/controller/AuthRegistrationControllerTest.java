package za.co.bonalabs.bonahr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterOrganisationAndOwner() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "organisationName": "Bona Test Company",
                    "legalName": "Bona Test Company Pty Ltd",
                    "registrationNumber": "REG-%s",
                    "taxNumber": "TAX-%s",
                    "organisationEmail": "company-%s@example.com",
                    "firstName": "Test",
                    "lastName": "Owner",
                    "email": "owner-%s@example.com",
                    "password": "SecurePassword123!"
                }
                """.formatted(
                uniqueValue,
                uniqueValue,
                uniqueValue,
                uniqueValue
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organisationId").isNotEmpty())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.roles[0]").value("OWNER"));
    }

    @Test
    void shouldPersistActiveOwnerWithHashedPassword() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        String ownerEmail = "owner-" + uniqueValue + "@example.com";

        String rawPassword = "SecurePassword123!";

        String request = """
                {
                    "organisationName": "Persistence Test Company",
                    "legalName": "Persistence Test Company Pty Ltd",
                    "registrationNumber": "REG-%s",
                    "taxNumber": "TAX-%s",
                    "organisationEmail": "company-%s@example.com",
                    "firstName": "Test",
                    "lastName": "Owner",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(
                uniqueValue,
                uniqueValue,
                uniqueValue,
                ownerEmail,
                rawPassword
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated());

        Organisation organisation =
                organisationRepository
                        .findByName("Persistence Test Company")
                        .orElseThrow();

        User owner =
                userRepository
                        .findByOrganisationIdAndEmailWithRoles(
                                organisation.getId(),
                                ownerEmail
                        )
                        .orElseThrow();

        assertEquals(
                UserStatus.ACTIVE,
                owner.getStatus()
        );

        assertEquals(
                organisation.getId(),
                owner.getOrganisation().getId()
        );

        assertNotEquals(
                rawPassword,
                owner.getPasswordHash()
        );

        assertTrue(
                passwordEncoder.matches(
                        rawPassword,
                        owner.getPasswordHash()
                )
        );

        assertTrue(
                owner.getRoles()
                        .stream()
                        .anyMatch(role ->
                                role.getName().equals("OWNER")
                        )
        );
    }

    @Test
    void shouldRejectRegistrationWithoutOrganisationName() throws Exception {

        String request = """
                {
                    "organisationName": "",
                    "legalName": "Invalid Company Pty Ltd",
                    "registrationNumber": "REG-INVALID",
                    "taxNumber": "TAX-INVALID",
                    "organisationEmail": "company@example.com",
                    "firstName": "Test",
                    "lastName": "Owner",
                    "email": "owner@example.com",
                    "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateOrganisationRegistrationNumber() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();
        String registrationNumber = "REG-" + uniqueValue;

        String firstRequest = """
                {
                    "organisationName": "First Company",
                    "legalName": "First Company Pty Ltd",
                    "registrationNumber": "%s",
                    "taxNumber": "TAX-FIRST-%s",
                    "organisationEmail": "first-%s@example.com",
                    "firstName": "First",
                    "lastName": "Owner",
                    "email": "first-owner-%s@example.com",
                    "password": "SecurePassword123!"
                }
                """.formatted(
                registrationNumber,
                uniqueValue,
                uniqueValue,
                uniqueValue
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequest)
                )
                .andExpect(status().isCreated());

        String secondRequest = """
                {
                    "organisationName": "Second Company",
                    "legalName": "Second Company Pty Ltd",
                    "registrationNumber": "%s",
                    "taxNumber": "TAX-SECOND-%s",
                    "organisationEmail": "second-%s@example.com",
                    "firstName": "Second",
                    "lastName": "Owner",
                    "email": "second-owner-%s@example.com",
                    "password": "SecurePassword123!"
                }
                """.formatted(
                registrationNumber,
                uniqueValue,
                uniqueValue,
                uniqueValue
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondRequest)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDuplicateOrganisationTaxNumber() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();
        String taxNumber = "TAX-" + uniqueValue;

        String firstRequest = """
                {
                    "organisationName": "First Tax Company",
                    "legalName": "First Tax Company Pty Ltd",
                    "registrationNumber": "REG-FIRST-%s",
                    "taxNumber": "%s",
                    "organisationEmail": "first-tax-%s@example.com",
                    "firstName": "First",
                    "lastName": "Owner",
                    "email": "first-tax-owner-%s@example.com",
                    "password": "SecurePassword123!"
                }
                """.formatted(
                uniqueValue,
                taxNumber,
                uniqueValue,
                uniqueValue
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequest)
                )
                .andExpect(status().isCreated());

        String secondRequest = """
                {
                    "organisationName": "Second Tax Company",
                    "legalName": "Second Tax Company Pty Ltd",
                    "registrationNumber": "REG-SECOND-%s",
                    "taxNumber": "%s",
                    "organisationEmail": "second-tax-%s@example.com",
                    "firstName": "Second",
                    "lastName": "Owner",
                    "email": "second-tax-owner-%s@example.com",
                    "password": "SecurePassword123!"
                }
                """.formatted(
                uniqueValue,
                taxNumber,
                uniqueValue,
                uniqueValue
        );

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondRequest)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldAllowRegisteredOwnerToLogin() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();
        String ownerEmail = "login-owner-" + uniqueValue + "@example.com";
        String password = "SecurePassword123!";

        String registrationRequest = """
                {
                    "organisationName": "Login Test Company",
                    "legalName": "Login Test Company Pty Ltd",
                    "registrationNumber": "REG-%s",
                    "taxNumber": "TAX-%s",
                    "organisationEmail": "company-%s@example.com",
                    "firstName": "Login",
                    "lastName": "Owner",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(
                uniqueValue,
                uniqueValue,
                uniqueValue,
                ownerEmail,
                password
        );

        String registrationResponse =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(registrationRequest)
                        )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String organisationId =
                objectMapper
                        .readTree(registrationResponse)
                        .get("organisationId")
                        .asText();

        String loginRequest = """
                {
                    "organisationId": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(
                organisationId,
                ownerEmail,
                password
        );

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.organisationId").value(organisationId))
                .andExpect(jsonPath("$.email").value(ownerEmail))
                .andExpect(jsonPath("$.roles[0]").value("OWNER"));
    }
}