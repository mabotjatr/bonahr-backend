package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.EmployeeLeaveAccount;
import za.co.bonalabs.bonahr.entity.LeaveAccrualMethod;
import za.co.bonalabs.bonahr.repository.EmployeeLeaveAccountRepository;
import za.co.bonalabs.bonahr.entity.EmployeeStatus;

import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MonthlyLeaveAccrualService {

    private final EmployeeLeaveAccountRepository employeeLeaveAccountRepository;
    private final LeaveBalanceService leaveBalanceService;
    private static final Logger log = LoggerFactory.getLogger(MonthlyLeaveAccrualService.class);

    public MonthlyLeaveAccrualService(EmployeeLeaveAccountRepository employeeLeaveAccountRepository, LeaveBalanceService leaveBalanceService) {
        this.employeeLeaveAccountRepository = employeeLeaveAccountRepository;

        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public void processMonthlyAccruals(LocalDate accrualDate) {

        for (EmployeeLeaveAccount account : employeeLeaveAccountRepository.findAllByLeavePolicyAccrualMethodAndLeavePolicyActiveTrueAndEmployeeStatus(
                LeaveAccrualMethod.MONTHLY, EmployeeStatus.ACTIVE)) {

            if (alreadyAccruedForMonth(account, accrualDate)) {
                continue;
            }

            try {
                leaveBalanceService.applyAccrual(
                        account.getOrganisation().getId(),
                        account.getId(),
                        account.getLeavePolicy().getAccrualRate(),
                        accrualDate,
                        null);
            } catch (RuntimeException ex) {
                log.error("Failed to apply monthly leave accrual for account {}", account.getId(), ex);
            }
        }
    }

    private boolean alreadyAccruedForMonth(EmployeeLeaveAccount account, LocalDate accrualDate) {

        LocalDate lastAccrualDate = account.getLastAccrualDate();

        if (lastAccrualDate == null) {
            return false;
        }

        return YearMonth.from(lastAccrualDate).equals(YearMonth.from(accrualDate));
    }
}