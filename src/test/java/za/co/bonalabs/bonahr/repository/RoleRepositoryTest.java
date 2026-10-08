package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import za.co.bonalabs.bonahr.entity.Role;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldLoadOwnerRole() {

        Optional<Role> role =
                roleRepository.findByName("OWNER");

        assertThat(role).isPresent();

        assertThat(role.get().getName())
                .isEqualTo("OWNER");

        assertThat(role.get().getDescription())
                .isNotBlank();
    }

    @Test
    void shouldLoadEmployeeRole() {

        Optional<Role> role =
                roleRepository.findByName("EMPLOYEE");

        assertThat(role).isPresent();

        assertThat(role.get().getName())
                .isEqualTo("EMPLOYEE");
    }
}