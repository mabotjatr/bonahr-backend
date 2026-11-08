package za.co.bonalabs.bonahr.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import za.co.bonalabs.bonahr.entity.Organisation;
import za.co.bonalabs.bonahr.entity.User;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringMailInvitationEmailSenderTest {

    @Test
    void shouldSendSetupInvitationEmail() {

        JavaMailSender mailSender = mock(JavaMailSender.class);

        SpringMailInvitationEmailSender sender = new SpringMailInvitationEmailSender(
                mailSender,
                "no-reply@bonahr.co.za",
                "http://localhost:3000/setup-password");

        Organisation organisation = new Organisation("BonaHR Test");

        User user = new User(organisation, "employee@example.com", "Jane", "Doe");

        sender.sendSetupInvitation(user, "raw-setup-token");

        verify(mailSender).send(argThat((SimpleMailMessage message) -> "no-reply@bonahr.co.za".equals(message.getFrom())
                && message.getTo() != null
                && message.getTo().length == 1
                && "employee@example.com".equals(message.getTo()[0])
                && "Set up your BonaHR account".equals(message.getSubject())
                && message.getText() != null
                && message.getText().contains("http://localhost:3000/setup-password?token=raw-setup-token")));
    }
}