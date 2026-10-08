package za.co.bonalabs.bonahr.controller.dto;

import java.util.List;
import java.util.UUID;

public record RegisterResponse(
        UUID organisationId,
        UUID userId,
        String email,
        List<String> roles
) {
}