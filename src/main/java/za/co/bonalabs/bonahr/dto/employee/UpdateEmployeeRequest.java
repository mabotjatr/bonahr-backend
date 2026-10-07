package za.co.bonalabs.bonahr.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmployeeRequest(

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @Email
        String email
) {
}