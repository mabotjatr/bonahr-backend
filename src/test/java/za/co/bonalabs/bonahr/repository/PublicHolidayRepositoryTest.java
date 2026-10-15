package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.PublicHoliday;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PublicHolidayRepositoryTest {

    @Autowired
    private PublicHolidayRepository publicHolidayRepository;

    @Test
    void shouldFindActiveSouthAfricanPublicHoliday() {

        LocalDate holidayDate = LocalDate.of(2099, 2, 3);

        publicHolidayRepository.saveAndFlush(new PublicHoliday("ZA", holidayDate, "Repository Test Holiday"));

        boolean exists = publicHolidayRepository.existsByCountryCodeAndHolidayDateAndActiveTrue("ZA", holidayDate);

        assertTrue(exists);
    }

    @Test
    void shouldIgnoreInactiveSouthAfricanPublicHoliday() {

        LocalDate holidayDate = LocalDate.of(2099, 2, 4);

        PublicHoliday holiday = new PublicHoliday("ZA", holidayDate, "Repository Test Holiday");

        holiday.setActive(false);

        publicHolidayRepository.saveAndFlush(holiday);

        boolean exists = publicHolidayRepository.existsByCountryCodeAndHolidayDateAndActiveTrue("ZA", holidayDate);

        assertFalse(exists);
    }

    @Test
    void shouldLoadSeeded2026SouthAfricanPublicHoliday() {

        boolean exists = publicHolidayRepository
                        .existsByCountryCodeAndHolidayDateAndActiveTrue(
                                "ZA",
                                LocalDate.of(2026, 11, 4)
                        );

        assertTrue(exists);
    }
}