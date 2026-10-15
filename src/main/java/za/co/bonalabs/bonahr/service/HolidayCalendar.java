package za.co.bonalabs.bonahr.service;

import java.time.LocalDate;

@FunctionalInterface
public interface HolidayCalendar {

    boolean isPublicHoliday(LocalDate date);
}