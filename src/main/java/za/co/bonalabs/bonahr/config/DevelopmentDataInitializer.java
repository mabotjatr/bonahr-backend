package za.co.bonalabs.bonahr.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.Role;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.repository.OrganisationRepository;
import za.co.bonalabs.bonahr.repository.RoleRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

@Configuration
@Profile("dev")
public class DevelopmentDataInitializer {

    @Bean
    CommandLineRunner createDevelopmentUser(
            OrganisationRepository organisationRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            Organisation organisation =
                    organisationRepository
                            .findByName("BonaLabs Demo")
                            .orElseGet(() ->
                                    organisationRepository.save(
                                            new Organisation("BonaLabs Demo")
                                    )
                            );

            Role role =
                    roleRepository.findByName("HR_ADMIN")
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "HR_ADMIN role does not exist"
                                    )
                            );

            if (userRepository
                    .findByOrganisationIdAndEmailWithRoles(
                            organisation.getId(),
                            "admin@bonalabs.local"
                    )
                    .isPresent()) {
                return;
            }

            User user = new User(
                    organisation,
                    "admin@bonalabs.local",
                    "BonaHR",
                    "Administrator"
            );

            user.setPasswordHash(
                    passwordEncoder.encode("Admin123!")
            );

            user.setStatus(UserStatus.ACTIVE);

            user.addRole(role);

            userRepository.save(user);
        };
    }
}