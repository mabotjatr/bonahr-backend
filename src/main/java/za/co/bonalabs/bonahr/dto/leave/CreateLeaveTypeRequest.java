package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotBlank;

public record CreateLeaveTypeRequest(

        @NotBlank
        String code,

        @NotBlank
        String name,

        boolean paid,

        boolean requiresApproval) {
}