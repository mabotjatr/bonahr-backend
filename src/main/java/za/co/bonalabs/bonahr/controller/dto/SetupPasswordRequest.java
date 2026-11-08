package za.co.bonalabs.bonahr.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupPasswordRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 12)
        String password

) {
}