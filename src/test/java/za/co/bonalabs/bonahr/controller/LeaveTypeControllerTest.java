package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.LeaveTypeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.security.JwtService;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeaveTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    void shouldCreateLeaveTypeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Leave Type API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "code": "annual",
                    "name": "Annual Leave",
                    "paid": true,
                    "requiresApproval": true
                }
                """;

        mockMvc.perform(post("/api/v1/leave-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.code").value("ANNUAL"))
                .andExpect(jsonPath("$.name").value("Annual Leave"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.requiresApproval").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(UUID.randomUUID(), organisationId, roles);
    }

    @Test
    void shouldForbidEmployeeRoleFromCreatingLeaveType() throws Exception {

        Organisation organisation = new Organisation("Leave Type Security Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "code": "ANNUAL",
                    "name": "Annual Leave",
                    "paid": true,
                    "requiresApproval": true
                }
                """;

        mockMvc.perform(post("/api/v1/leave-types")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnLeaveTypesForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Leave List API Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave List API Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "SICK", "Sick Leave", true, true));

        leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        // Must never appear for organisation A
        leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "FAMILY", "Family Responsibility Leave", true, true));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-types").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("ANNUAL"))
                .andExpect(jsonPath("$[0].name").value("Annual Leave"))
                .andExpect(jsonPath("$[1].code").value("SICK"))
                .andExpect(jsonPath("$[1].name").value("Sick Leave"));
    }

    @Test
    void shouldReturnLeaveTypeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Leave Type Detail API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "SICK", "Sick Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/leave-types/" + leaveType.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.code").value("SICK"))
                .andExpect(jsonPath("$.name").value("Sick Leave"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.requiresApproval").value(true))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldNotReturnLeaveTypeFromAnotherOrganisation() throws Exception {

        Organisation organisationA = new Organisation("Leave Type Detail Tenant A " + UUID.randomUUID());

        Organisation organisationB = new Organisation("Leave Type Detail Tenant B " + UUID.randomUUID());

        organisationA = organisationRepository.saveAndFlush(organisationA);

        organisationB = organisationRepository.saveAndFlush(organisationB);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisationB.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/leave-types/" + leaveType.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateLeaveTypeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Leave Type Update API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "name": "Annual Paid Leave",
                    "paid": true,
                    "requiresApproval": false
                }
                """;

        mockMvc.perform(put("/api/v1/leave-types/" + leaveType.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.code").value("ANNUAL"))
                .andExpect(jsonPath("$.name").value("Annual Paid Leave"))
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.requiresApproval").value(false))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldForbidEmployeeRoleFromUpdatingLeaveType() throws Exception {

        Organisation organisation = new Organisation("Leave Type Update Security Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "name": "Changed Annual Leave",
                    "paid": false,
                    "requiresApproval": false
                }
                """;

        mockMvc.perform(put("/api/v1/leave-types/" + leaveType.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());

    }

    @Test
    void shouldDeactivateLeaveTypeForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Leave Type Status API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-types/" + leaveType.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.code").value("ANNUAL"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldForbidEmployeeRoleFromUpdatingLeaveTypeStatus() throws Exception {

        Organisation organisation = new Organisation("Leave Type Status Security Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-types/" + leaveType.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

}