package za.co.bonalabs.bonahr.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.bonalabs.bonahr.dto.user.LinkEmployeeRequest;
import za.co.bonalabs.bonahr.dto.user.UserEmployeeLinkResponse;
import za.co.bonalabs.bonahr.entity.User;
import za.co.bonalabs.bonahr.security.JwtAuthenticationDetails;
import za.co.bonalabs.bonahr.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{userId}/employee")
    public ResponseEntity<UserEmployeeLinkResponse> linkEmployee(@PathVariable UUID userId, @Valid @RequestBody LinkEmployeeRequest request, Authentication authentication) {

        JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();

        User user = userService.linkEmployee(details.organisationId(), userId, request.employeeId());

        UserEmployeeLinkResponse response = new UserEmployeeLinkResponse(user.getId(), user.getEmployee().getId());

        return ResponseEntity.ok(response);
    }
}