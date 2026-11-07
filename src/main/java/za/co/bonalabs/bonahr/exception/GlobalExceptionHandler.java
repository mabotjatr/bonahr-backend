package za.co.bonalabs.bonahr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException exception) {
        return new ErrorResponse("RESOURCE_NOT_FOUND", exception.getMessage(), OffsetDateTime.now());
    }

    public record ErrorResponse(String code, String message, OffsetDateTime timestamp) {
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void handleInvalidCredentials() {
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateResource(DuplicateResourceException exception) {
        return new ErrorResponse("RESOURCE_CONFLICT", exception.getMessage(), OffsetDateTime.now());
    }

    @ExceptionHandler(InvalidUserProvisioningException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidUserProvisioning(InvalidUserProvisioningException exception) {
        return new ErrorResponse("INVALID_USER_PROVISIONING", exception.getMessage(), OffsetDateTime.now());
    }
}