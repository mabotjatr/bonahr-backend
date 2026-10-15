package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Component;
import za.co.bonalabs.bonahr.repository.PublicHolidayRepository;

import java.time.LocalDate;

@Component
public class DatabaseHolidayCalendar implements HolidayCalendar {

    private static final String SOUTH_AFRICA_COUNTRY_CODE = "ZA";

    private final PublicHolidayRepository publicHolidayRepository;

    public DatabaseHolidayCalendar(PublicHolidayRepository publicHolidayRepository) {
        this.publicHolidayRepository = publicHolidayRepository;
    }

    @Override
    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidayRepository.existsByCountryCodeAndHolidayDateAndActiveTrue(SOUTH_AFRICA_COUNTRY_CODE, date);
    }
}