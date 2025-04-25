package io.credable.lms.exception;

import io.credable.lms.model.LoanStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:45 pm
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(value = { LoanApplicationException.class })
    protected ResponseEntity<Object> handleLoanAppException(LoanApplicationException ex, WebRequest request) {
        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.BAD_REQUEST;
        if (ex.getStatusHint() == LoanStatus.FAILED_CONCURRENT) {
            status = org.springframework.http.HttpStatus.CONFLICT;
        }
        return ResponseEntity.status(status).body(Map.of("status", "FAILED", "message", ex.getMessage()));
    }

    @ExceptionHandler(value = { ResourceNotFoundException.class })
    protected ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(Map.of("status", "NOT_FOUND", "message", ex.getMessage()));
    }

    // Generic fallback handler
    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<Object> handleGeneric(Exception ex, WebRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "An unexpected internal error occurred."));
    }
}
