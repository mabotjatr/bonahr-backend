package za.co.bonalabs.bonahr.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bonahr.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        String issuer
) {
}