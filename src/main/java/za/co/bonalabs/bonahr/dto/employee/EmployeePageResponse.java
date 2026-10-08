package za.co.bonalabs.bonahr.dto.employee;

import java.util.List;

public record EmployeePageResponse(
        List<EmployeeResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}