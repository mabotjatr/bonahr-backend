package za.co.bonalabs.bonahr.controller.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        UUID organisationId,
        String email,
        List<String> roles
) {
}