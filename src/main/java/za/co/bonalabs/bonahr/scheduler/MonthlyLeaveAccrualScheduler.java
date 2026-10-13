package za.co.bonalabs.bonahr.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import za.co.bonalabs.bonahr.service.MonthlyLeaveAccrualService;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class MonthlyLeaveAccrualScheduler {

    private static final ZoneId SOUTH_AFRICA_ZONE = ZoneId.of("Africa/Johannesburg");

    private final MonthlyLeaveAccrualService monthlyLeaveAccrualService;

    public MonthlyLeaveAccrualScheduler(MonthlyLeaveAccrualService monthlyLeaveAccrualService) {
        this.monthlyLeaveAccrualService = monthlyLeaveAccrualService;
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "Africa/Johannesburg")
    public void processMonthlyAccruals() {

        LocalDate accrualDate = LocalDate.now(SOUTH_AFRICA_ZONE);

        monthlyLeaveAccrualService.processMonthlyAccruals(accrualDate);
    }
}