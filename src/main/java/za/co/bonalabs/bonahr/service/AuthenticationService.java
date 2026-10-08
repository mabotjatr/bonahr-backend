package za.co.bonalabs.bonahr.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.controller.dto.RegisterRequest;
import za.co.bonalabs.bonahr.controller.dto.RegisterResponse;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.RoleRepository;
import za.co.bonalabs.bonahr.controller.dto.LoginRequest;
import za.co.bonalabs.bonahr.controller.dto.LoginResponse;
import za.co.bonalabs.bonahr.exception.InvalidCredentialsException;
import za.co.bonalabs.bonahr.repository.UserRepository;
import za.co.bonalabs.bonahr.security.JwtService;
import za.co.bonalabs.bonahr.exception.DuplicateResourceException;


import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OrganisationRepository organisationRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            OrganisationRepository organisationRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.organisationRepository = organisationRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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
                .sorted()
                .toList();

        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getOrganisation().getId(),
                roles
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getOrganisation().getId(),
                user.getEmail(),
                roles
        );
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (request.registrationNumber() != null
                && !request.registrationNumber().isBlank()
                && organisationRepository.existsByRegistrationNumber(
                request.registrationNumber())) {

            throw new DuplicateResourceException(
                    "An organisation with registration number "
                            + request.registrationNumber()
                            + " already exists"
            );
        }

        if (request.taxNumber() != null
                && !request.taxNumber().isBlank()
                && organisationRepository.existsByTaxNumber(
                request.taxNumber() )) {

            throw new DuplicateResourceException(
                    "An organisation with tax number "
                            + request.taxNumber()
                            + " already exists"
            );
        }

        Organisation organisation = new Organisation(request.organisationName());

        organisation.setLegalName(request.legalName());
        organisation.setRegistrationNumber(request.registrationNumber());
        organisation.setTaxNumber(request.taxNumber());
        organisation.setEmail(request.organisationEmail());

        organisation = organisationRepository.save(organisation);

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OWNER role is not configured"
                        )
                );

        User owner = new User(
                organisation,
                request.email(),
                request.firstName(),
                request.lastName()
        );

        owner.setPasswordHash(passwordEncoder.encode(request.password()));

        owner.setStatus(UserStatus.ACTIVE);
        owner.addRole(ownerRole);

        owner = userRepository.save(owner);

        return new RegisterResponse(
                organisation.getId(),
                owner.getId(),
                owner.getEmail(),
                List.of("OWNER")
        );
    }
}