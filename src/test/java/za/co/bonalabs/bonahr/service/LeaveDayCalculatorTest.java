package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import za.co.bonalabs.bonahr.entity.LeaveDayPortion;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaveDayCalculatorTest {

    private final LeaveDayCalculator leaveDayCalculator = new LeaveDayCalculator();

    @Test
    void shouldExcludeSaturdayAndSundayFromLeaveDays() {

        BigDecimal result = leaveDayCalculator.calculate(LocalDate.of(2026, 11, 6),  // Friday
                LocalDate.of(2026, 11, 9)   // Monday
        );

        assertEquals(BigDecimal.valueOf(2), result);
    }

    @Test
    void shouldReturnZeroWhenLeaveFallsOnlyOnWeekend() {

        BigDecimal result = leaveDayCalculator.calculate(LocalDate.of(2026, 11, 7),  // Saturday
                LocalDate.of(2026, 11, 8)   // Sunday
        );

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldCountSingleWeekdayAsOneLeaveDay() {

        BigDecimal result = leaveDayCalculator.calculate(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9));

        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    void shouldExcludeConfiguredPublicHolidayFromLeaveDays() {

        HolidayCalendar holidayCalendar = date -> date.equals(LocalDate.of(2026, 11, 10));

        LeaveDayCalculator calculator = new LeaveDayCalculator(holidayCalendar);

        BigDecimal result = calculator.calculate(LocalDate.of(2026, 11, 9),   // Monday
                LocalDate.of(2026, 11, 13)   // Friday
        );

        assertEquals(BigDecimal.valueOf(4), result);
    }

    @Test
    void shouldCalculateHalfDayLeaveAsHalfDay() {

        BigDecimal result = leaveDayCalculator.calculate(
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 9),
                LeaveDayPortion.HALF_DAY_AM);

        assertEquals(new BigDecimal("0.5"), result);
    }

    @Test
    void shouldRejectHalfDayLeaveSpanningMultipleDates() {

        assertThrows(
                IllegalArgumentException.class,
                () -> leaveDayCalculator.calculate(
                        LocalDate.of(2026, 11, 9),
                        LocalDate.of(2026, 11, 10),
                        LeaveDayPortion.HALF_DAY_AM
                )
        );
    }
}