package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganisationControllerTest {

    @Autowired
    private MockMvc mockMvc;

   /* @Test
    void shouldCreateOrganisation() throws Exception {

        String request = """
                {
                    "name": "Test Company",
                    "legalName": "Test Company Pty Ltd",
                    "registrationNumber": "2026/TEST-00123",
                    "taxNumber": "TEST-TAX-00123",
                    "email": "test@example.com",
                    "phone": "+27 11 123 4567",
                    "website": "https://example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/organisations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Test Company"))
                .andExpect(jsonPath("$.legalName")
                        .value("Test Company Pty Ltd"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }*/

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = extractId(response);

        mockMvc.perform(get("/api/v1/organisations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Lookup Company"));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrganisation() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/organisations/" + unknownId))
                .andExpect(status().isNotFound());
    }

    private String extractId(String response) {
        int start = response.indexOf("\"id\":\"") + 6;
        int end = response.indexOf("\"", start);

        return response.substring(start, end);
    }
}