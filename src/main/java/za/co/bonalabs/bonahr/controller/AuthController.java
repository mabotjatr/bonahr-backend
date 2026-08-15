package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.bonalabs.bonahr.controller.dto.LoginRequest;
import za.co.bonalabs.bonahr.controller.dto.LoginResponse;
import za.co.bonalabs.bonahr.service.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(
                authenticationService.login(request)
        );
    }
}