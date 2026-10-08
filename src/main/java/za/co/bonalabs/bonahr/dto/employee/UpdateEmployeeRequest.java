package za.co.bonalabs.bonahr.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import za.co.bonalabs.bonahr.entity.EmploymentType;

import java.time.LocalDate;

public record UpdateEmployeeRequest(

        @NotBlank String firstName,

        @NotBlank String lastName,

        @Email String email,

        String jobTitle,

        String department,

        EmploymentType employmentType,

        LocalDate startDate,

        String phone) {

    // Keeps existing tests/callers working.
    public UpdateEmployeeRequest(String firstName, String lastName, String email) {
        this(firstName, lastName, email, null, null, null, null, null);
    }
}