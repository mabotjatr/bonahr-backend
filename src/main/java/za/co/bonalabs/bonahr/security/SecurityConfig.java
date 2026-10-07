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

                        .requestMatchers( HttpMethod.POST,"/api/v1/organisations")
                        .denyAll()

                        .requestMatchers(HttpMethod.GET,"/api/v1/organisations/**")
                        .hasAnyRole("OWNER","HR_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/employees")
                        .hasAnyRole("OWNER", "HR_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/employees/**")
                        .hasAnyRole("OWNER", "HR_ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**")
                        .hasAnyRole("OWNER", "HR_ADMIN")

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