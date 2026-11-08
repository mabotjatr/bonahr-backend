package za.co.bonalabs.bonahr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import za.co.bonalabs.bonahr.entity.User;

@Service
@ConditionalOnProperty(name = "bonahr.mail.enabled", havingValue = "true")
public class SpringMailInvitationEmailSender implements InvitationEmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String setupPasswordUrl;

    public SpringMailInvitationEmailSender(JavaMailSender mailSender,
                                           @Value("${bonahr.mail.from}") String fromAddress,
                                           @Value("${bonahr.frontend.setup-password-url}") String setupPasswordUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.setupPasswordUrl = setupPasswordUrl;
    }

    @Override
    public void sendSetupInvitation(User user, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Set up your BonaHR account");

        message.setText("""
                Hello %s,
                
                Your BonaHR account has been created.
                
                Set up your password using the link below:
                
                %s?token=%s
                
                This invitation will expire in 24 hours.
                """.formatted(user.getFirstName(), setupPasswordUrl, rawToken));

        mailSender.send(message);
    }
}