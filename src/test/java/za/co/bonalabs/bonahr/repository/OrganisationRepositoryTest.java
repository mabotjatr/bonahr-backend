package za.co.bonalabs.bonahr.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.Organisation;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrganisationRepositoryTest {

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void shouldSaveAndRetrieveOrganisation() {

        Organisation organisation = new Organisation("BonaLabs");

        organisation.setLegalName("BonaLabs Pty Ltd");
        organisation.setEmail("info@bonalabs.co.za");

        Organisation saved = organisationRepository.save(organisation);

        assertThat(saved.getId()).isNotNull();

        Organisation retrieved = organisationRepository.findById(saved.getId()).orElseThrow();

        assertThat(retrieved.getName())
                .isEqualTo("BonaLabs");

        assertThat(retrieved.getLegalName())
                .isEqualTo("BonaLabs Pty Ltd");

        assertThat(retrieved.getEmail())
                .isEqualTo("info@bonalabs.co.za");

        assertThat(retrieved.getStatus())
                .isEqualTo(
                        za.co.bonalabs.bonahr.entity.OrganisationStatus.ACTIVE
                );
    }
}