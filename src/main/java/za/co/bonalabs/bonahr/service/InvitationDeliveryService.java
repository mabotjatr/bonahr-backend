package za.co.bonalabs.bonahr.service;

import za.co.bonalabs.bonahr.entity.User;

public interface InvitationDeliveryService {

    void sendSetupInvitation(User user, String rawToken);
}