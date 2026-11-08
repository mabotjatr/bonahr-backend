package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class SetupInvitationTransactionIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private InvitationEmailSender emailSender;

    @Test
    void shouldSendInvitationOnlyAfterTransactionCommits() {

        User user = createUser();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {

            eventPublisher.publishEvent(new SetupInvitationCreatedEvent(user, "raw-token"));

            verifyNoInteractions(emailSender);
        });

        verify(emailSender).sendSetupInvitation(user, "raw-token");
    }

    @Test
    void shouldNotSendInvitationWhenTransactionRollsBack() {

        User user = createUser();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {

            eventPublisher.publishEvent(new SetupInvitationCreatedEvent(user, "raw-token"));

            status.setRollbackOnly();
        });

        verifyNoInteractions(emailSender);
    }

    private User createUser() {

        Organisation organisation = new Organisation("BonaHR Test");

        return new User(organisation, "employee@example.com", "Jane", "Doe");
    }
}