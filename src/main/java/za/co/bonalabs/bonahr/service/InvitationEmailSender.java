package za.co.bonalabs.bonahr.service;

import za.co.bonalabs.bonahr.entity.User;

public interface InvitationEmailSender {

    void sendSetupInvitation(User user, String rawToken);
}