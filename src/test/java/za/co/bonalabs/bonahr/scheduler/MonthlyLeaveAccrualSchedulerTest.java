package za.co.bonalabs.bonahr.scheduler;

import org.junit.jupiter.api.Test;
import za.co.bonalabs.bonahr.service.MonthlyLeaveAccrualService;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MonthlyLeaveAccrualSchedulerTest {

    @Test
    void shouldTriggerMonthlyLeaveAccrualProcessing() {

        MonthlyLeaveAccrualService monthlyLeaveAccrualService = mock(MonthlyLeaveAccrualService.class);

        MonthlyLeaveAccrualScheduler scheduler = new MonthlyLeaveAccrualScheduler(monthlyLeaveAccrualService);

        scheduler.processMonthlyAccruals();

        LocalDate expectedDate = LocalDate.now(ZoneId.of("Africa/Johannesburg"));

        verify(monthlyLeaveAccrualService).processMonthlyAccruals(expectedDate);
    }
}