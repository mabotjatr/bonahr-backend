package za.co.bonalabs.bonahr.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.repository.UserRepository;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String authenticationIdentifier)
            throws UsernameNotFoundException {

        String[] parts = authenticationIdentifier.split(":", 2);

        if (parts.length != 2) {
            throw new UsernameNotFoundException(
                    "Invalid authentication identifier"
            );
        }

        UUID organisationId;

        try {
            organisationId = UUID.fromString(parts[0]);
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException(
                    "Invalid organisation ID"
            );
        }

        String email = parts[1];

        User user = userRepository
                .findByOrganisationIdAndEmailWithRoles(
                        organisationId,
                        email
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        var authorities = user.getRoles()
                .stream()
                .map(role ->
                        new SimpleGrantedAuthority(
                                "ROLE_" + role.getName()
                        )
                )
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User
                .withUsername(authenticationIdentifier)
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(
                        user.getStatus() != UserStatus.ACTIVE
                )
                .build();
    }
}