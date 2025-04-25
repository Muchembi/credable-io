package io.credable.lms.exception;

import io.credable.lms.model.LoanStatus;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:42 pm
 */
public class LoanApplicationException extends RuntimeException  {
    private LoanStatus statusHint;

    public LoanApplicationException(String message) {
        super(message);
    }
    public LoanApplicationException(String message, LoanStatus statusHint) {
        super(message);
        this.statusHint = statusHint;
    }
    public LoanApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoanStatus getStatusHint() { return statusHint; }
}
