package za.co.bonalabs.bonahr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidEmployeeStatusTransitionException extends RuntimeException {

    public InvalidEmployeeStatusTransitionException(String message) {
        super(message);
    }
}