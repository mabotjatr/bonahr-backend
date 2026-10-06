package za.co.bonalabs.bonahr.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String createAccessToken() {
        return jwtService.generateToken(
                TEST_USER_ID,
                TEST_ORGANISATION_ID,
                List.of("HR_ADMIN")
        );
    }

    @Test
    void shouldCreateOrganisation() throws Exception {

        String uniqueValue = UUID.randomUUID().toString();

        String request = """
                {
                    "name": "Test Company",
                    "legalName": "Test Company Pty Ltd",
                    "registrationNumber": "TEST-%s",
                    "taxNumber": "TAX-%s",
                    "email": "test@example.com",
                    "phone": "+27 11 123 4567",
                    "website": "https://example.com"
                }
                """.formatted(uniqueValue, uniqueValue);

        mockMvc.perform(post("/api/v1/organisations")
                        .header(
                                "Authorization",
                                "Bearer " + createAccessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test Company"))
                .andExpect(jsonPath("$.legalName")
                        .value("Test Company Pty Ltd"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectOrganisationWithoutName() throws Exception {

        String request = """
                {
                    "name": "",
                    "legalName": "Test Company Pty Ltd",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/organisations")
                        .header(
                                "Authorization",
                                "Bearer " + createAccessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOrganisationById() throws Exception {

        String request = """
                {
                    "name": "Lookup Company",
                    "legalName": "Lookup Company Pty Ltd",
                    "email": "lookup@example.com"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/organisations")
                        .header(
                                "Authorization",
                                "Bearer " + createAccessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = extractId(response);

        mockMvc.perform(get("/api/v1/organisations/" + id)
                        .header(
                                "Authorization",
                                "Bearer " + createAccessToken()
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Lookup Company"));
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

    private String extractId(String response) {
        int start = response.indexOf("\"id\":\"") + 6;
        int end = response.indexOf("\"", start);

        return response.substring(start, end);
    }
}