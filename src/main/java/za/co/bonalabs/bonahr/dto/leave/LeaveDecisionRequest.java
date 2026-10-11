package za.co.bonalabs.bonahr.dto.leave;

import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(
        @Size(max = 1000) String comment
) {
}