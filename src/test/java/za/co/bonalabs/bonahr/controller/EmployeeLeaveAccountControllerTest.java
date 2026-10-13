package za.co.bonalabs.bonahr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.*;
import za.co.bonalabs.bonahr.repository.*;
import za.co.bonalabs.bonahr.security.JwtService;
import za.co.bonalabs.bonahr.service.LeaveBalanceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeLeaveAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeavePolicyRepository leavePolicyRepository;

    @Autowired
    private EmployeeLeaveAccountRepository employeeLeaveAccountRepository;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Test
    void shouldCreateEmployeeLeaveAccountForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account API Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "employeeId": "%s",
                    "leaveTypeId": "%s"
                }
                """.formatted(employee.getId(), leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-accounts").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.employeeName").value("John Doe"))
                .andExpect(jsonPath("$.leaveTypeId").value(leaveType.getId().toString()))
                .andExpect(jsonPath("$.leaveTypeCode").value("ANNUAL"))
                .andExpect(jsonPath("$.leaveTypeName").value("Annual Leave"))
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.reservedDays").value(0));
    }

    private String createAccessToken(UUID organisationId, List<String> roles) {
        return jwtService.generateToken(UUID.randomUUID(), organisationId, roles);
    }

    @Test
    void shouldForbidEmployeeRoleFromCreatingLeaveAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account Security Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "employeeId": "%s",
                    "leaveTypeId": "%s"
                }
                """.formatted(employee.getId(), leaveType.getId());

        mockMvc.perform(post("/api/v1/leave-accounts").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnLeaveAccountsForAuthenticatedOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Account List Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Account List Tenant B " + UUID.randomUUID()));

        Employee employeeA = employeeRepository.saveAndFlush(new Employee(organisationA, "EMP-001", "John", "Doe"));

        Employee employeeB = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-002", "Jane", "Doe"));

        LeaveType leaveTypeA = leaveTypeRepository.saveAndFlush(new LeaveType(organisationA, "ANNUAL", "Annual Leave", true, true));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policyA = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationA, leaveTypeA, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        LeavePolicy policyB = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationB, leaveTypeB, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisationA, employeeA, leaveTypeA, policyA));

        // Must never appear for organisation A
        employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisationB, employeeB, leaveTypeB, policyB));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-accounts").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].employeeNumber").value("EMP-001")).andExpect(jsonPath("$[0].employeeName").value("John Doe")).andExpect(jsonPath("$[0].leaveTypeCode").value("ANNUAL")).andExpect(jsonPath("$[0].currentBalance").value(0)).andExpect(jsonPath("$[0].reservedDays").value(0));
    }

    @Test
    void shouldReturnLeaveAccountForAuthenticatedOrganisation() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account Detail Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-accounts/{id}", account.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.employeeName").value("John Doe"))
                .andExpect(jsonPath("$.leaveTypeCode").value("ANNUAL"))
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.reservedDays").value(0));
    }

    @Test
    void shouldReturnNotFoundForLeaveAccountFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Leave Account Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Leave Account Tenant B " + UUID.randomUUID()));

        Employee employeeB = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-002", "Jane", "Doe"));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policyB = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationB, leaveTypeB, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount accountB = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisationB, employeeB, leaveTypeB, policyB));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-accounts/{id}", accountB.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldForbidEmployeeRoleFromListingLeaveAccounts() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Account Read Security Company " + UUID.randomUUID()));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        mockMvc.perform(get("/api/v1/leave-accounts").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void shouldApplyManualLeaveBalanceAdjustment() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Manual Adjustment Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        String request = """
                {
                    "amount": 2.5000,
                    "comment": "Opening balance correction"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-accounts/{id}/adjustments", account.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.currentBalance").value(2.5))
                .andExpect(jsonPath("$.reservedDays").value(0));
    }

    @Test
    void shouldForbidEmployeeRoleFromApplyingManualAdjustment() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Manual Adjustment Security Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        String token = createAccessToken(organisation.getId(), List.of("EMPLOYEE"));

        String request = """
                {
                    "amount": 2.5000,
                    "comment": "Should not be allowed"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-accounts/{id}/adjustments", account.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnLeaveBalanceTransactionsForAccount() throws Exception {

        Organisation organisation = organisationRepository.saveAndFlush(new Organisation("Leave Ledger Company " + UUID.randomUUID()));

        Employee employee = employeeRepository.saveAndFlush(new Employee(organisation, "EMP-001", "John", "Doe"));

        LeaveType leaveType = leaveTypeRepository.saveAndFlush(new LeaveType(organisation, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy leavePolicy = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisation, leaveType, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount account = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisation, employee, leaveType, leavePolicy));

        leaveBalanceService.applyManualAdjustment(organisation.getId(), account.getId(), new BigDecimal("2.5000"), UUID.randomUUID(), "Opening balance correction");

        String token = createAccessToken(organisation.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-accounts/{id}/transactions", account.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].transactionType").value("MANUAL_ADJUSTMENT"))
                .andExpect(jsonPath("$[0].amount").value(2.5))
                .andExpect(jsonPath("$[0].balanceAfter").value(2.5))
                .andExpect(jsonPath("$[0].reservedAfter").value(0))
                .andExpect(jsonPath("$[0].comment").value("Opening balance correction"));
    }

    @Test
    void shouldReturnNotFoundForTransactionsFromAnotherOrganisation() throws Exception {

        Organisation organisationA = organisationRepository.saveAndFlush(new Organisation("Ledger Tenant A " + UUID.randomUUID()));

        Organisation organisationB = organisationRepository.saveAndFlush(new Organisation("Ledger Tenant B " + UUID.randomUUID()));

        Employee employeeB = employeeRepository.saveAndFlush(new Employee(organisationB, "EMP-002", "Jane", "Doe"));

        LeaveType leaveTypeB = leaveTypeRepository.saveAndFlush(new LeaveType(organisationB, "ANNUAL", "Annual Leave", true, true));

        LeavePolicy policyB = leavePolicyRepository.saveAndFlush(new LeavePolicy(organisationB, leaveTypeB, LeaveAccrualMethod.MONTHLY, new BigDecimal("1.2500"), false, BigDecimal.ZERO));

        EmployeeLeaveAccount accountB = employeeLeaveAccountRepository.saveAndFlush(new EmployeeLeaveAccount(organisationB, employeeB, leaveTypeB, policyB));

        String token = createAccessToken(organisationA.getId(), List.of("HR_ADMIN"));

        mockMvc.perform(get("/api/v1/leave-accounts/{id}/transactions", accountB.getId()).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }
}