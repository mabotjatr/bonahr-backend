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
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.repository.LeavePolicyRepository;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeavePolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Test
    void shouldCreateLeavePolicyForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = new Organisation("Leave Policy API Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "leaveTypeId": "%s",
                    "accrualMethod": "MONTHLY",
                    "accrualRate": 1.2500,
                    "allowNegative": true,
                    "negativeLimitDays": 3.0000
                }
                """.formatted(leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-policies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.leaveTypeId").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeCode").value("ANNUAL"))
                .andExpect(jsonPath("$.leaveTypeName").value("Annual Leave"))
                .andExpect(jsonPath("$.accrualMethod").value("MONTHLY"))
                .andExpect(jsonPath("$.accrualRate").value(1.25))
                .andExpect(jsonPath("$.allowNegative").value(true))
                .andExpect(jsonPath("$.negativeLimitDays").value(3.0))
                .andExpect(jsonPath("$.active").value(true));
    }

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(UUID.randomUUID(), organisationId, roles);
    }

    @Test
    void shouldForbidEmployeeRoleFromCreatingLeavePolicy() throws Exception {

        Organisation organisation = new Organisation("Leave Policy Security Company " + UUID.randomUUID());

        organisation = organisationRepository.saveAndFlush(organisation);

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "leaveTypeId": "%s",
                    "accrualMethod": "MONTHLY",
                    "accrualRate": 1.2500,
                    "allowNegative": false,
                    "negativeLimitDays": 0
                }
                """.formatted(leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-policies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnLeavePoliciesForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Policy List Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Policy List Tenant B " + UUID.randomUUID()));

        LeaveType annualLeave = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveType otherTenantLeave = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "SICK", "Sick Leave", true, true));

        leavePolicyRepository.saveAndFlush(new LeavePolicy(
                organisationA,
                annualLeave,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        // Must never appear for organisation A
        leavePolicyRepository.saveAndFlush(new LeavePolicy(
                organisationB,
                otherTenantLeave,
                LeaveAccrualMethod.MONTHLY,
                new BigDecimal("1.0000"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-policies")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].leaveTypeCode").value("ANNUAL"))
                .andExpect(jsonPath("$[0].leaveTypeName").value("Annual Leave"))
                .andExpect(jsonPath("$[0].accrualMethod").value("MONTHLY"))
                .andExpect(jsonPath("$[0].accrualRate").value(1.25));
    }

    @Test
    void shouldReturnLeavePolicyForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Policy Detail Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), true, new BigDecimal("3.0000")));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-policies/{id}", leavePolicy.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leavePolicy.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeId").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeCode").value("ANNUAL"))
                .andExpect(jsonPath("$.accrualMethod").value("MONTHLY"))
                .andExpect(jsonPath("$.allowNegative").value(true))
                .andExpect(jsonPath("$.negativeLimitDays").value(3.0));
    }

    @Test
    void shouldUpdateLeavePolicyForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Policy Update Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "accrualMethod": "UPFRONT",
                    "accrualRate": 15.0000,
                    "allowNegative": true,
                    "negativeLimitDays": 3.0000
                }
                """;

        mockMvc.perform(put("/api/v1/leave-policies/{id}", leavePolicy.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leavePolicy.getId().toString()))
                .andExpect(jsonPath("$.accrualMethod").value("UPFRONT"))
                .andExpect(jsonPath("$.accrualRate").value(15.0))
                .andExpect(jsonPath("$.allowNegative").value(true))
                .andExpect(jsonPath("$.negativeLimitDays").value(3.0));
    }

    @Test
    void shouldForbidEmployeeRoleFromUpdatingLeavePolicy() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Policy Update Security Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "accrualMethod": "UPFRONT",
                    "accrualRate": 15.0000,
                    "allowNegative": false,
                    "negativeLimitDays": 0
                }
                """;

        mockMvc.perform(put("/api/v1/leave-policies/{id}", leavePolicy.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeactivateLeavePolicyForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Policy Status Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-policies/{id}/status", leavePolicy.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leavePolicy.getId().toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldForbidEmployeeRoleFromUpdatingLeavePolicyStatus() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Policy Status Security Company " + UUID.randomUUID()));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-policies/{id}/status", leavePolicy.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundForLeavePolicyFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Policy Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Policy Tenant B " + UUID.randomUUID()));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policyB = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationB, leaveTypeB, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-policies/{id}", policyB.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}