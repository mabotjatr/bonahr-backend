package za.co.bonalabs.bonahr.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        String organisationName,

        String legalName,

        String registrationNumber,

        String taxNumber,

        @Email
        String organisationEmail,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 12, message = "Password must be at least 12 characters")
        String password
) {
}