package za.co.bonalabs.bonahr.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    @Test
    void shouldCreateJwtProperties() {

        JwtProperties properties = new JwtProperties("test-secret", 60, "bonahr-api");

        assertThat(properties.secret()).isEqualTo("test-secret");

        assertThat(properties.expirationMinutes()).isEqualTo(60);

        assertThat(properties.issuer()).isEqualTo("bonahr-api");
    }
}