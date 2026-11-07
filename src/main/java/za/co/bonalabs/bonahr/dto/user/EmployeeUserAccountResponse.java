package za.co.bonalabs.bonahr.dto.user;

import java.util.List;
import java.util.UUID;

public record EmployeeUserAccountResponse(
        UUID userId,
        UUID employeeId,
        String email,
        String status,
        List<String> roles
) {
}