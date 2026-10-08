package za.co.bonalabs.bonahr.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private AutoCloseable mocks;

    private final UUID userId =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final UUID organisationId =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        filter = new JwtAuthenticationFilter(jwtService);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing()
            throws Exception {

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer()
            throws Exception {

        var request = new MockHttpServletRequest();
        request.addHeader(
                "Authorization",
                "Basic some-value"
        );

        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verifyNoInteractions(jwtService);
    }

    @Test
    void shouldAuthenticateValidToken()
            throws Exception {

        var request = new MockHttpServletRequest();
        request.addHeader(
                "Authorization",
                "Bearer valid-token"
        );

        var response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);

        when(claims.getSubject())
                .thenReturn(userId.toString());

        when(claims.get(
                "organisationId",
                String.class
        )).thenReturn(organisationId.toString());

        when(claims.get(
                "roles",
                List.class
        )).thenReturn(List.of("HR_ADMIN"));

        when(jwtService.parseToken("valid-token"))
                .thenReturn(claims);

        filter.doFilter(request, response, filterChain);

        var authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.getPrincipal())
                .isEqualTo(userId);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_HR_ADMIN");

        assertThat(authentication.getDetails())
                .isInstanceOf(JwtAuthenticationDetails.class);

        var details =
                (JwtAuthenticationDetails)
                        authentication.getDetails();

        assertThat(details.userId())
                .isEqualTo(userId);

        assertThat(details.organisationId())
                .isEqualTo(organisationId);

        verify(jwtService)
                .parseToken("valid-token");

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateInvalidToken()
            throws Exception {

        var request = new MockHttpServletRequest();
        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );

        var response = new MockHttpServletResponse();

        when(jwtService.parseToken("invalid-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();

        verify(jwtService)
                .parseToken("invalid-token");

        verify(filterChain)
                .doFilter(request, response);
    }
}