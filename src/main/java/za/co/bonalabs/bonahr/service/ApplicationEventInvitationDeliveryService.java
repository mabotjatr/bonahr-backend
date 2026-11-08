package za.co.bonalabs.bonahr.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import za.co.bonalabs.bonahr.entity.User;

@Service
public class ApplicationEventInvitationDeliveryService implements InvitationDeliveryService {

    private final ApplicationEventPublisher eventPublisher;

    public ApplicationEventInvitationDeliveryService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendSetupInvitation(User user, String rawToken) {
        eventPublisher.publishEvent(new SetupInvitationCreatedEvent(user, rawToken));
    }
}