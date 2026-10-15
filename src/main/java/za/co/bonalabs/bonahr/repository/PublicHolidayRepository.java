package za.co.bonalabs.bonahr.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.bonalabs.bonahr.entity.PublicHoliday;

import java.time.LocalDate;
import java.util.UUID;

public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, UUID> {

    boolean existsByCountryCodeAndHolidayDateAndActiveTrue(String countryCode, LocalDate holidayDate);
}