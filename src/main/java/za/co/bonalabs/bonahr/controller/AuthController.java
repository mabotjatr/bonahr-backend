package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.controller.dto.*;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import za.co.bonalabs.bonahr.service.UserInvitationService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserInvitationService userInvitationService;

    public AuthController(AuthenticationService authenticationService,UserInvitationService userInvitationService) {
        this.authenticationService = authenticationService;
        this.userInvitationService = userInvitationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register( @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authenticationService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/setup-password")
    public ResponseEntity<SetupPasswordResponse> setupPassword(@Valid @RequestBody SetupPasswordRequest request) {

        User user = userInvitationService.consumeInvitation(request.token(), request.password());

        return ResponseEntity.ok(new SetupPasswordResponse(user.getId(), user.getStatus().name()));
    }
}