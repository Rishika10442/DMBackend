package com.rishika.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleResourceNotFoundException(ResourceNotFoundException ex) {
        return "Error: Resource not found " + ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleGenericException(Exception ex) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("An error occurred.\n");

        // Add exception type
        errorMessage.append("Exception: ").append(ex.getClass().getSimpleName()).append("\n");

        // Add exception message if available
        if (ex.getMessage() != null) {
            errorMessage.append("Message: ").append(ex.getMessage()).append("\n");
        } else {
            errorMessage.append("Message: No specific message available.\n");
        }

        return errorMessage.toString();
    }

}
