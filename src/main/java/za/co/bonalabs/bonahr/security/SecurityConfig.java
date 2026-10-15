package za.co.bonalabs.bonahr.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    public static final String API_V_1_EMPLOYEES = "/api/v1/employees/**";
    public static final String OWNER = "OWNER";
    public static final String HR_ADMIN = "HR_ADMIN";
    public static final String API_V_1_ORGANISATIONS = "/api/v1/organisations";
    public static final String API_V_1_LEAVE_TYPES_POST = "/api/v1/leave-types";
    public static final String API_V_1_LEAVE_TYPES_UPDATE = "/api/v1/leave-types/**";
    public static final String API_V_1_LEAVE_REQUESTS = "/api/v1/leave-requests";
    public static final String API_V_1_LEAVE_REQUESTS_UPDATE = "/api/v1/leave-requests/**";
    public static final String API_V_1_LEAVE_POLICIES = "/api/v1/leave-policies";
    public static final String API_V_1_LEAVE_POLICIES_UPDATE = "/api/v1/leave-policies/**";
    public static final String API_V_1_LEAVE_ACCOUNTS_POST = "/api/v1/leave-accounts";
    public static final String API_V_1_LEAVE_ACCOUNTS = "/api/v1/leave-accounts/**";
    public static final String EMPLOYEE = "EMPLOYEE";
    public static final String API_V_1_LEAVE_REQUESTS_ME = "/api/v1/leave-requests/me";

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService
    ) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/health")
                        .permitAll()

                        .requestMatchers( HttpMethod.POST, API_V_1_ORGANISATIONS)
                        .denyAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/organisations/**")
                        .hasAnyRole(OWNER, SecurityConfig.HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, "/api/v1/employees")
                        .hasAnyRole(OWNER, HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_EMPLOYEES)
                        .hasAnyRole(OWNER, HR_ADMIN)

                        .requestMatchers(HttpMethod.GET, API_V_1_EMPLOYEES)
                        .hasAnyRole(OWNER, HR_ADMIN)

                        .requestMatchers(HttpMethod.PUT, API_V_1_EMPLOYEES)
                        .hasAnyRole(OWNER, HR_ADMIN)

                        .requestMatchers(HttpMethod.PATCH, API_V_1_EMPLOYEES)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.DELETE, API_V_1_EMPLOYEES)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers( HttpMethod.POST, API_V_1_LEAVE_TYPES_POST)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers( HttpMethod.PUT, API_V_1_LEAVE_TYPES_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers( HttpMethod.PATCH, API_V_1_LEAVE_TYPES_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_REQUESTS)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.GET, API_V_1_LEAVE_REQUESTS, API_V_1_LEAVE_REQUESTS_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.PATCH, API_V_1_LEAVE_REQUESTS_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_POLICIES)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.PUT, API_V_1_LEAVE_POLICIES_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.PATCH, API_V_1_LEAVE_POLICIES_UPDATE)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_ACCOUNTS_POST)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_ACCOUNTS)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.GET, API_V_1_LEAVE_ACCOUNTS)
                        .hasAnyRole(OWNER,HR_ADMIN)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_REQUESTS_ME)
                        .hasRole(EMPLOYEE)

                        .requestMatchers(HttpMethod.POST, API_V_1_LEAVE_REQUESTS)
                        .hasAnyRole(OWNER, HR_ADMIN)

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED
                                        )
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}