package za.co.bonalabs.bonahr.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
        String password
) {
}