package za.co.bonalabs.bonahr.security;

import java.util.UUID;

public record JwtAuthenticationDetails(
        UUID userId,
        UUID organisationId
) {
}