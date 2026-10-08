package za.co.bonalabs.bonahr.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private final UUID userId =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID organisationId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        JwtProperties properties =
                new JwtProperties(
                        "this-is-a-test-secret-that-is-long-enough-for-hs256",
                        60,
                        "bonahr-api"
                );

        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateToken() {

        String token = jwtService.generateToken(
                userId,
                organisationId,
                List.of("HR_ADMIN")
        );

        assertThat(token)
                .isNotBlank();
    }

    @Test
    void shouldParseValidToken() {

        String token = jwtService.generateToken(
                userId,
                organisationId,
                List.of("HR_ADMIN")
        );

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject())
                .isEqualTo(userId.toString());

        assertThat(claims.get("organisationId", String.class))
                .isEqualTo(organisationId.toString());

        assertThat(claims.getIssuer())
                .isEqualTo("bonahr-api");

        assertThat(claims.getExpiration())
                .isAfter(claims.getIssuedAt());
    }

    @Test
    void shouldRejectTamperedToken() {

        String token = jwtService.generateToken(
                userId,
                organisationId,
                List.of("HR_ADMIN")
        );

        String tamperedToken =
                token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(
                () -> jwtService.parseToken(tamperedToken)
        ).isInstanceOf(RuntimeException.class);
    }
}