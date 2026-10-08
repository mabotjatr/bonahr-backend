package za.co.bonalabs.bonahr.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import za.co.bonalabs.bonahr.entity.EmployeeDocumentType;

public record CreateEmployeeDocumentRequest(

        @NotNull
        EmployeeDocumentType documentType,

        @NotBlank
        String fileName,

        @NotBlank
        String storageKey,

        String mimeType,

        Long fileSize
) {
}