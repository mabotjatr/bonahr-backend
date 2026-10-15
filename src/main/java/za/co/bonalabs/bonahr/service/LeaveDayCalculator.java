package za.co.bonalabs.bonahr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import za.co.bonalabs.bonahr.entity.LeaveDayPortion;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class LeaveDayCalculator {


    private final HolidayCalendar holidayCalendar;

    public LeaveDayCalculator() {
        this(date -> false);
    }

    @Autowired
    public LeaveDayCalculator(HolidayCalendar holidayCalendar) {
        this.holidayCalendar = holidayCalendar;
    }

    public BigDecimal calculate(LocalDate startDate, LocalDate endDate) {

        long workingDays = 0;

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {

            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            boolean publicHoliday = holidayCalendar.isPublicHoliday(currentDate);

            if (!weekend && !publicHoliday) {
                workingDays++;
            }

            currentDate = currentDate.plusDays(1);
        }

        return BigDecimal.valueOf(workingDays);
    }

    public BigDecimal calculate(LocalDate startDate, LocalDate endDate, LeaveDayPortion dayPortion) {

        if (dayPortion == LeaveDayPortion.FULL_DAY) {
            return calculate(startDate, endDate);
        }

        if (!startDate.equals(endDate)) {
            throw new IllegalArgumentException("Half-day leave must start and end on the same date");
        }

        BigDecimal fullDayValue = calculate(startDate, endDate);

        if (fullDayValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal("0.5");
    }
}