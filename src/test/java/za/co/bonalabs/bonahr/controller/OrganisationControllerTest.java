package za.co.bonalabs.bonahr.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrganisationControllerTest {

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_ORGANISATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_ORGANISATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    private Organisation createTestOrganisation(String name) {

        Organisation organisation = new Organisation(name);

        organisation.setLegalName(name + " Pty Ltd");
        organisation.setEmail(
                UUID.randomUUID() + "@example.com"
        );

        return organisationRepository.saveAndFlush(organisation);
    }

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(
                TEST_USER_ID,
                organisationId,
                roles
        );
    }

    private String createAccessToken(UUID organisationId) {
        return createAccessToken(organisationId, List.of("HR_ADMIN")
        );
    }

    private String createAccessToken() {
        return createAccessToken(TEST_ORGANISATION_ID);
    }

    @Test
    void shouldReturnForbiddenWhenHrAdminCreatesOrganisation()
            throws Exception {

        String request = """
            {
                "name": "Test Company",
                "legalName": "Test Company Pty Ltd",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/organisations")
                                .header(
                                        "Authorization",
                                        "Bearer " + createAccessToken()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOrganisationById() throws Exception {

        Organisation organisation = createTestOrganisation("Lookup Company");

        String token = createAccessToken(organisation.getId());

        mockMvc.perform(
                        get("/api/v1/organisations/" + organisation.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(organisation.getId().toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Lookup Company")
                );
    }

    @Test
    void shouldNotReturnOrganisationFromAnotherTenant() throws Exception {

        Organisation organisation = createTestOrganisation("Tenant B Company");

        String otherTenantToken = createAccessToken(OTHER_ORGANISATION_ID);

        mockMvc.perform(
                        get("/api/v1/organisations/" + organisation.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + otherTenantToken
                                )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForUnknownOrganisation() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/organisations/" + unknownId)
                        .header(
                                "Authorization",
                                "Bearer " + createAccessToken()
                        ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUnauthorizedWhenCreatingOrganisationWithoutToken() throws Exception {

        String request = """
                {
                    "name": "Unauthorised Company",
                    "legalName": "Unauthorised Company Pty Ltd",
                    "email": "unauthorised@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/organisations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() throws Exception {

        mockMvc.perform(get("/api/v1/organisations/" + UUID.randomUUID())
                        .header(
                                "Authorization",
                                "Bearer invalid-token"
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeReadsOrganisationDetails() throws Exception {

        Organisation organisation = createTestOrganisation("Role Test Company");

        String employeeToken = createAccessToken(
                organisation.getId(),
                List.of("EMPLOYEE")
        );

        mockMvc.perform(
                        get("/api/v1/organisations/" + organisation.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + employeeToken
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOwnerToReadOrganisationDetails() throws Exception {

        Organisation organisation = createTestOrganisation("Owner Test Company");

        String ownerToken = createAccessToken(
                organisation.getId(),
                List.of("OWNER")
        );

        mockMvc.perform(
                        get("/api/v1/organisations/" + organisation.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + ownerToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(organisation.getId().toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Owner Test Company")
                );
    }

    @Test
    void shouldReturnForbiddenWhenEmployeeCreatesOrganisation() throws Exception {

        String employeeToken = createAccessToken(
                TEST_ORGANISATION_ID,
                List.of("EMPLOYEE")
        );

        String request = """
                {
                    "name": "Another Company",
                    "legalName": "Another Company Pty Ltd",
                    "email": "another@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/organisations")
                        .header(
                                "Authorization",
                                "Bearer " + employeeToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    private String extractId(String response) {
        int start = response.indexOf("\"id\":\"") + 6;
        int end = response.indexOf("\"", start);

        return response.substring(start, end);
    }
}