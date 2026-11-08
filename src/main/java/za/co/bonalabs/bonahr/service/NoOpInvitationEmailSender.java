package za.co.bonalabs.bonahr.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import za.co.bonalabs.bonahr.entity.User;

@Service
@ConditionalOnProperty(name = "bonahr.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpInvitationEmailSender implements InvitationEmailSender {

    @Override
    public void sendSetupInvitation(User user, String rawToken) {
        // Intentionally disabled when email delivery is not configured.
    }
}