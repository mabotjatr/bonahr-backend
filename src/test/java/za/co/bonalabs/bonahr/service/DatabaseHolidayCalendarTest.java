package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import za.co.bonalabs.bonahr.repository.PublicHolidayRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DatabaseHolidayCalendarTest {

    @Test
    void shouldReturnTrueWhenDateIsActivePublicHoliday() {

        PublicHolidayRepository repository = Mockito.mock(PublicHolidayRepository.class);

        DatabaseHolidayCalendar calendar = new DatabaseHolidayCalendar(repository);

        LocalDate holiday = LocalDate.of(2026, 12, 25);

        when(repository.existsByCountryCodeAndHolidayDateAndActiveTrue("ZA", holiday)).thenReturn(true);

        assertTrue(calendar.isPublicHoliday(holiday));
    }

    @Test
    void shouldReturnFalseWhenDateIsNotPublicHoliday() {

        PublicHolidayRepository repository = Mockito.mock(PublicHolidayRepository.class);

        DatabaseHolidayCalendar calendar = new DatabaseHolidayCalendar(repository);

        LocalDate normalDay = LocalDate.of(2026, 12, 24);

        when(repository.existsByCountryCodeAndHolidayDateAndActiveTrue("ZA", normalDay)).thenReturn(false);

        assertFalse(calendar.isPublicHoliday(normalDay));
    }
}