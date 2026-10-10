package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.NotNull;

public record UpdateLeaveTypeStatusRequest(

        @NotNull
        Boolean active
) {
}