package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotBlank;

public record UpdateLeaveTypeRequest(

        @NotBlank
        String name,

        boolean paid,

        boolean requiresApproval
) {
}