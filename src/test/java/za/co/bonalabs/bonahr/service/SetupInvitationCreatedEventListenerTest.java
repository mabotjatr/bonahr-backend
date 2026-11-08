package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SetupInvitationCreatedEventListenerTest {

    @Test
    void shouldSendInvitationEmailWhenEventIsHandled() {

        InvitationEmailSender emailSender = mock(InvitationEmailSender.class);

        SetupInvitationCreatedEventListener listener = new SetupInvitationCreatedEventListener(emailSender);

        Organisation organisation = new Organisation("BonaHR Test");

        User user = new User(organisation, "employee@example.com", "Jane", "Doe");

        SetupInvitationCreatedEvent event = new SetupInvitationCreatedEvent(user, "raw-setup-token");

        listener.handle(event);

        verify(emailSender).sendSetupInvitation(user, "raw-setup-token");
    }
}