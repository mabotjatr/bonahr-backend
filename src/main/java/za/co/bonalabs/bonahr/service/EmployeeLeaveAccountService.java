package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.dto.leave.CreateEmployeeLeaveAccountRequest;
import za.co.bonalabs.bonahr.entity.Employee;
import za.co.bonalabs.bonahr.entity.EmployeeLeaveAccount;
import za.co.bonalabs.bonahr.entity.LeavePolicy;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;
import za.co.bonalabs.bonahr.repository.LeavePolicyRepository;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeLeaveAccountService {

    private final EmployeeLeaveAccountRepository employeeLeaveAccountRepository;
    private final LeavePolicyRepository leavePolicyRepository;
    private final EmployeeService employeeService;

    public EmployeeLeaveAccountService(EmployeeLeaveAccountRepository employeeLeaveAccountRepository, LeavePolicyRepository leavePolicyRepository, EmployeeService employeeService) {
        this.employeeLeaveAccountRepository = employeeLeaveAccountRepository;
        this.leavePolicyRepository = leavePolicyRepository;
        this.employeeService = employeeService;
    }

    @Transactional
    public EmployeeLeaveAccount createEmployeeLeaveAccount(UUID organisationId, CreateEmployeeLeaveAccountRequest request) {

        Employee employee = employeeService.getEmployee(organisationId, request.employeeId());

        LeavePolicy leavePolicy = leavePolicyRepository.findByOrganisationIdAndLeaveTypeId(
                organisationId,
                request.leaveTypeId()).orElseThrow(() -> new ResourceNotFoundException("Leave policy not found for leave type: " + request.leaveTypeId()));

        boolean accountExists = employeeLeaveAccountRepository.findByOrganisationIdAndEmployeeIdAndLeaveTypeId(
                organisationId,
                request.employeeId(),
                request.leaveTypeId()).isPresent();

        if (accountExists) {
            throw new IllegalStateException("Leave account already exists for employee: " + request.employeeId() + " and leave type: " + request.leaveTypeId());
        }

        EmployeeLeaveAccount account = new EmployeeLeaveAccount(employee.getOrganisation(), employee, leavePolicy.getLeaveType(), leavePolicy);

        return employeeLeaveAccountRepository.saveAndFlush(account);
    }

    @Transactional(readOnly = true)
    public EmployeeLeaveAccount getEmployeeLeaveAccount(UUID organisationId, UUID accountId) {
        return employeeLeaveAccountRepository.findByIdAndOrganisationId(
                accountId, organisationId).orElseThrow(() -> new ResourceNotFoundException("Leave account not found: " + accountId));
    }

    @Transactional(readOnly = true)
    public List<EmployeeLeaveAccount> getEmployeeLeaveAccounts(UUID organisationId) {
        return employeeLeaveAccountRepository.findAllByOrganisationId(organisationId);
    }
}