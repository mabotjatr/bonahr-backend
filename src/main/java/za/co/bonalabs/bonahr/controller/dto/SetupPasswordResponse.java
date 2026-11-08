package za.co.bonalabs.bonahr.controller.dto;

import java.util.UUID;

public record SetupPasswordResponse(
        UUID userId,
        String status
) {
}