package io.credable.lms.controller;

import io.credable.lms.dto.LoanRequest;
import io.credable.lms.dto.LoanStatusResponse;
import io.credable.lms.dto.SubscriptionRequest;
import io.credable.lms.exception.LoanApplicationException;
import io.credable.lms.exception.ResourceNotFoundException;
import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import io.credable.lms.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:46 pm
 */
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@Slf4j
public class MobileAppController {
    private final LoanService loanService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody @Valid SubscriptionRequest request) {
        if (request == null || request.getCustomerNumber() == null || request.getCustomerNumber().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Customer number is required."));
        }
        try {
            loanService.subscribeCustomer(request.getCustomerNumber());
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Customer verified and subscribed."));
        } catch (LoanApplicationException e) {
            log.warn("Subscription failed for {}: {}", request.getCustomerNumber(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "FAILED", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during subscription for {}", request.getCustomerNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "An internal error occurred."));
        }
    }

    @PostMapping("/loans/request")
    public ResponseEntity<?> requestLoan(@RequestBody @Valid LoanRequest request) {
        if (request == null || request.getCustomerNumber() == null || request.getCustomerNumber().isBlank() || request.getAmount() == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Customer number and amount are required."));
        }
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Loan amount must be positive."));
        }

        try {
            LoanApplication application = loanService.requestLoan(request);
            return ResponseEntity.accepted().body(Map.of( // Use 202 Accepted
                    "status", application.getStatus().toString(), // PENDING_SCORE
                    "message", "Loan application submitted. Scoring is in progress.",
                    "applicationId", application.getId()
            ));
        } catch (LoanApplicationException e) {
            log.warn("Loan request failed for {}: {}", request.getCustomerNumber(), e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST; // Default for app logic errors
            if (e.getStatusHint() == LoanStatus.FAILED_CONCURRENT) {
                status = HttpStatus.CONFLICT; // Use 409 Conflict for concurrent requests
            }
            return ResponseEntity.status(status).body(Map.of("status", "FAILED", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during loan request for {}", request.getCustomerNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "An internal error occurred."));
        }
    }

    @GetMapping("/loans/status/{customerNumber}")
    public ResponseEntity<?> getLoanStatus(@PathVariable String customerNumber) {
        if (customerNumber == null || customerNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "Customer number is required."));
        }
        try {
            LoanApplication application = loanService.getLoanStatus(customerNumber);
            LoanStatusResponse response = mapToLoanStatusResponse(application); // Helper method
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.info("Loan status check for non-existent application: {}", customerNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "NOT_FOUND",
                    "message", e.getMessage()
            ));
        }
        catch (Exception e) {
            log.error("Unexpected error fetching status for {}", customerNumber, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "ERROR", "message", "An internal error occurred."));
        }
    }

    // Helper to map internal model to response DTO
    private LoanStatusResponse mapToLoanStatusResponse(LoanApplication app) {
        LoanStatusResponse resp = new LoanStatusResponse();
        resp.setCustomerNumber(app.getCustomerNumber());
        resp.setStatus(app.getStatus().toString());
        resp.setApplicationId(app.getId());
        resp.setRequestedAmount(app.getRequestedAmount());
        resp.setLimitAmount(app.getLimitAmount());
        resp.setScore(app.getScore());

        // Set appropriate message based on status
        switch (app.getStatus()) {
            case PENDING_SCORE:
            case SCORING_IN_PROGRESS:
                resp.setMessage("Scoring is in progress.");
                break;
            case SCORING_FAILED:
                resp.setMessage(app.getFailureMessage() != null ? app.getFailureMessage() : "Could not retrieve score. Please try applying again later.");
                break;
            case APPROVED:
                resp.setMessage("Loan approved.");
                break;
            case ACTIVE: // If ACTIVE is used
                resp.setMessage("Loan is active.");
                // Add more details like disbursed amount, due date if available
                break;
            case REJECTED_LIMIT:
                resp.setMessage(app.getFailureMessage() != null ? app.getFailureMessage() : "Loan application rejected. Requested amount exceeds limit.");
                break;
            case REJECTED_EXCLUSION:
                resp.setMessage(app.getFailureMessage() != null ? app.getFailureMessage() : "Loan application rejected due to exclusion.");
                break;
            case REJECTED_KYC_FAILED:
                resp.setMessage(app.getFailureMessage() != null ? app.getFailureMessage() : "Loan application rejected due to KYC validation failure.");
                break;
            case ELIGIBLE:
                resp.setMessage("Customer is eligible to apply for a loan.");
                break;
            // Add other cases as needed
            default:
                resp.setMessage("Status: " + app.getStatus());
        }
        return resp;
    }
}
