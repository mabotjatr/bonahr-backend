package za.co.bonalabs.bonahr.controller;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import za.co.bonalabs.bonahr.dto.leave.CreateLeaveRequestRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.LeaveRequest;
import za.co.bonalabs.bonahr.entity.LeaveType;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.repository.EmployeeRepository;
import za.co.bonalabs.bonahr.repository.LeaveTypeRepository;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.security.JwtService;
import za.co.bonalabs.bonahr.service.LeaveRequestService;
import za.co.bonalabs.bonahr.service.LeaveBalanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaveRequestControllerTest {

    @MockitoBean
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveRequestService leaveRequestService;

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(UUID.randomUUID(), organisationId, roles);
    }

    @Test
    void shouldCreateLeaveRequestAsHrAdmin() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s",
                    "leaveTypeId": "%s",
                    "startDate": "2026-12-07",
                    "endDate": "2026-12-11",
                    "reason": "December leave"
                }
                """.formatted(employee.getId(), leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-requests").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeId").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.dayPortion").value("FULL_DAY"))
                .andExpect(jsonPath("$.startDate").value("2026-12-07"))
                .andExpect(jsonPath("$.endDate").value("2026-12-11"))
                .andExpect(jsonPath("$.reason").value("December leave"));
    }

    @Test
    void shouldForbidEmployeeRoleFromCreatingLeaveRequest() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Security Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "employeeId": "%s",
                    "leaveTypeId": "%s",
                    "startDate": "2026-12-07",
                    "endDate": "2026-12-11",
                    "reason": "December leave"
                }
                """.formatted(employee.getId(), leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListLeaveRequestsForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request List API A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request List API B " + UUID.randomUUID()));

        Employee employeeA = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        Employee employeeB = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-" + UUID.randomUUID(), "John", "Smith"));

        LeaveType leaveTypeA = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        leaveRequestService.createLeaveRequest(organisationA.getId(), UUID.randomUUID(), new CreateLeaveRequestRequest(employeeA.getId(), leaveTypeA.getId(), LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), "Later leave"));

        leaveRequestService.createLeaveRequest(organisationA.getId(), UUID.randomUUID(), new CreateLeaveRequestRequest(employeeA.getId(), leaveTypeA.getId(), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5), "Earlier leave"));

        leaveRequestService.createLeaveRequest(organisationB.getId(), UUID.randomUUID(), new CreateLeaveRequestRequest(employeeB.getId(), leaveTypeB.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "Other tenant leave"));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organisationId").value(organisationA.getId().toString()))
                .andExpect(jsonPath("$[0].startDate").value("2026-11-03"))
                .andExpect(jsonPath("$[1].startDate").value("2026-12-10"));
    }

    @Test
    void shouldForbidEmployeeRoleFromListingLeaveRequests() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request List Security Company " + UUID.randomUUID()));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/leave-requests").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void shouldGetLeaveRequestForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Detail API " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-requests/" + leaveRequest.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveRequest.getId().toString()))
                .andExpect(jsonPath("$.organisationId").value(organisation.getId().toString()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeId").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("December leave"));
    }

    @Test
    void shouldForbidEmployeeRoleFromGettingLeaveRequest() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Request Detail Security " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/leave-requests/" + leaveRequest.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundForLeaveRequestFromDifferentOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Request Detail Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Request Detail Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisationA.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisationB.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-requests/" + leaveRequest.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldApproveLeaveRequestAsHrAdmin() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Approval API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "comment": "Approved"
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-requests/" + leaveRequest.getId() + "/approve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionComment").value("Approved"))
                .andExpect(jsonPath("$.decidedByUserId").exists())
                .andExpect(jsonPath("$.decidedAt").exists());
    }

    @Test
    void shouldForbidEmployeeRoleFromApprovingLeaveRequest() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Approval Security Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "comment": "Approved"
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-requests/" + leaveRequest.getId() + "/approve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLeaveRequestAsHrAdmin() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Rejection API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "comment": "Insufficient staffing"
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-requests/" + leaveRequest.getId() + "/reject")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionComment").value("Insufficient staffing"))
                .andExpect(jsonPath("$.decidedByUserId").exists())
                .andExpect(jsonPath("$.decidedAt").exists());
    }

    @Test
    void shouldCancelPendingLeaveRequestAsHrAdmin() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Cancellation API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisation.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(patch("/api/v1/leave-requests/" + leaveRequest.getId() + "/cancel")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveRequest.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturnNotFoundWhenApprovingLeaveRequestFromDifferentOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Approval Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Approval Tenant B " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveRequest leaveRequest = leaveRequestService.createLeaveRequest(
                organisationA.getId(),
                UUID.randomUUID(),
                new CreateLeaveRequestRequest(
                        employee.getId(),
                        leaveType.getId(),
                        LocalDate.of(2026, 12, 7),
                        LocalDate.of(2026, 12, 11),
                        "December leave"));

        String token = createAccessToken(organisationB.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "comment": "Approved"
                }
                """;

        mockMvc.perform(patch("/api/v1/leave-requests/" + leaveRequest.getId() + "/approve")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateHalfDayLeaveRequestAsHrAdmin() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Half Day Leave API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-" + UUID.randomUUID(), "Jane", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s",
                    "leaveTypeId": "%s",
                    "startDate": "2026-11-09",
                    "endDate": "2026-11-09",
                    "dayPortion": "HALF_DAY_AM",
                    "reason": "Morning appointment"
                }
                """.formatted(employee.getId(), leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayPortion").value("HALF_DAY_AM"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startDate").value("2026-11-09"))
                .andExpect(jsonPath("$.endDate").value("2026-11-09"));
    }
}
