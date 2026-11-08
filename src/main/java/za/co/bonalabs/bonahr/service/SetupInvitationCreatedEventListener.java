package za.co.bonalabs.bonahr.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SetupInvitationCreatedEventListener {

    private final InvitationEmailSender emailSender;

    public SetupInvitationCreatedEventListener(InvitationEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SetupInvitationCreatedEvent event) {
        emailSender.sendSetupInvitation(event.user(), event.rawToken());
    }
}