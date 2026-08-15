package za.co.bonalabs.bonahr.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import za.co.bonalabs.bonahr.controller.dto.LoginRequest;
import za.co.bonalabs.bonahr.controller.dto.LoginResponse;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.exception.InvalidCredentialsException;
import za.co.bonalabs.bonahr.repository.UserRepository;

import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {

        String authenticationIdentifier = request.organisationId() + ":" + request.email();

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationIdentifier, request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByOrganisationIdAndEmailWithRoles(
                        request.organisationId(),
                        request.email())
                .orElseThrow(InvalidCredentialsException::new);

        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .toList();

        return new LoginResponse(
                user.getId(),
                user.getOrganisation().getId(),
                user.getEmail(),
                roles
        );
    }
}