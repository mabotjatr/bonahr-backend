package za.co.bonalabs.bonahr.dto.organisation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganisationRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 200)
        String legalName,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 100)
        String taxNumber,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 255)
        String website
) {
}