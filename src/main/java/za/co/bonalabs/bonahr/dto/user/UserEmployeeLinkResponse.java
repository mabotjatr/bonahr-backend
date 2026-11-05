package za.co.bonalabs.bonahr.dto.user;

import java.util.UUID;

public record UserEmployeeLinkResponse(UUID userId, UUID employeeId) {
}