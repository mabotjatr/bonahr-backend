package za.co.bonalabs.bonahr.service;

import za.co.bonalabs.bonahr.entity.User;

public record SetupInvitationCreatedEvent(
        User user,
        String rawToken
) {
}