package za.co.bonalabs.bonahr.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.entity.UserInvitation;
import za.co.bonalabs.bonahr.entity.UserStatus;
import za.co.bonalabs.bonahr.exception.InvalidUserInvitationException;
import za.co.bonalabs.bonahr.exception.ResourceNotFoundException;
import za.co.bonalabs.bonahr.repository.UserInvitationRepository;
import za.co.bonalabs.bonahr.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class UserInvitationService {

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRY_HOURS = 24;

    private final UserRepository userRepository;
    private final UserInvitationRepository userInvitationRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public UserInvitationService(UserRepository userRepository, UserInvitationRepository userInvitationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userInvitationRepository = userInvitationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreatedInvitation createInvitation(UUID organisationId, UUID userId) {

        User user = userRepository.findByIdAndOrganisationId(userId, organisationId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.getStatus() != UserStatus.INVITED) {
            throw new IllegalStateException("Only invited users can receive a setup invitation");
        }

        OffsetDateTime now = OffsetDateTime.now();

        userInvitationRepository.findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(user.getId())
                .forEach(invitation -> invitation.revoke(now));

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        UserInvitation invitation = new UserInvitation(user, tokenHash, now.plusHours(EXPIRY_HOURS));

        invitation = userInvitationRepository.saveAndFlush(invitation);

        return new CreatedInvitation(invitation, rawToken);
    }

    @Transactional
    public User consumeInvitation(String rawToken, String rawPassword) {

        String tokenHash = hashToken(rawToken);

        UserInvitation invitation = userInvitationRepository.findByTokenHash(tokenHash).orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        OffsetDateTime now = OffsetDateTime.now();

        if (!invitation.isUsableAt(now)) {
            throw new InvalidUserInvitationException("Invitation is no longer usable");
        }

        User user = invitation.getUser();

        if (user.getStatus() != UserStatus.INVITED) {
            throw new InvalidUserInvitationException(
                    "Invitation is no longer usable"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        user.setStatus(UserStatus.ACTIVE);

        invitation.markUsed(now);

        userRepository.saveAndFlush(user);
        userInvitationRepository.saveAndFlush(invitation);

        return user;
    }

    private String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record CreatedInvitation(UserInvitation invitation, String rawToken) {
    }
}