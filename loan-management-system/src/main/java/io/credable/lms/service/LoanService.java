package io.credable.lms.service;

import io.credable.lms.dto.CbsKycResponse;
import io.credable.lms.dto.LoanRequest;
import io.credable.lms.exception.LoanApplicationException;
import io.credable.lms.exception.ResourceNotFoundException;
import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import io.credable.lms.repo.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:39 pm
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class LoanService {
    private final LoanApplicationRepository loanRepo;
    private final CbsService cbsService; // Mocked
    private final ScoringService scoringService;

    // --- Subscription ---
    public LoanApplication subscribeCustomer(String customerNumber) {
        log.info("Attempting subscription for customer: {}", customerNumber);
        CbsKycResponse kyc = cbsService.getCustomerKyc(customerNumber);

        if (kyc == null || !"ACTIVE".equalsIgnoreCase(kyc.getStatus())) {
            log.warn("KYC check failed or customer inactive for {}", customerNumber);
            // Optionally save a failed status, or just throw
            LoanApplication failedApp = new LoanApplication(customerNumber);
            failedApp.setStatus(LoanStatus.REJECTED_KYC_FAILED);
            failedApp.setFailureMessage(kyc == null ? "Customer not found" : "Customer status not ACTIVE");
            // Decide whether to save failed subscriptions - for now, we don't persist just for KYC fail
            // loanRepo.save(failedApp);
            throw new LoanApplicationException(failedApp.getFailureMessage());
        }

        // Customer found and active, create/update record
        LoanApplication app = loanRepo.findByCustomerNumber(customerNumber)
                .orElse(new LoanApplication(customerNumber));
        app.setStatus(LoanStatus.ELIGIBLE); // Mark as eligible after KYC
        loanRepo.save(app);
        log.info("Customer {} subscribed successfully. Status: {}", customerNumber, app.getStatus());
        return app;
    }


    // --- Loan Request ---
    // Use @Transactional if using a database
    public LoanApplication requestLoan(LoanRequest request) {
        String customerNumber = request.getCustomerNumber();
        log.info("Processing loan request for customer: {} amount: {}", customerNumber, request.getAmount());

        // 1. Check for existing blocking loan/application using the lock mechanism
        if (!loanRepo.lockCustomerForProcessing(customerNumber)) {
            log.warn("Customer {} already has an active process or loan.", customerNumber);
            // Find the existing application to return its status
            LoanApplication existingApp = loanRepo.findByCustomerNumber(customerNumber)
                    .orElseThrow(() -> new IllegalStateException("Lock held but no application found for " + customerNumber)); // Should not happen

            // Throw specific exception or return a specific status DTO
            throw new LoanApplicationException(
                    "Cannot process request: An active loan or pending application exists. Status: " + existingApp.getStatus(),
                    LoanStatus.FAILED_CONCURRENT // Pass status hint
            );
        }

        // Lock acquired, proceed
        try {
            // 2. Basic Validation (Add more as needed)
            if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new LoanApplicationException("Invalid loan amount requested.");
            }

            // 3. Check KYC again? Optional, depends on flow. Assume subscribed status is sufficient.
            LoanApplication application = loanRepo.findByCustomerNumber(customerNumber)
                    .orElseThrow(() -> new LoanApplicationException("Customer not subscribed or found.")); // Must exist if lock acquired? Or handle creation here.

            // Re-check status just before proceeding (belt and suspenders)
            if (LoanStatus.ELIGIBLE != application.getStatus() && LoanStatus.SCORING_FAILED != application.getStatus()
                    && LoanStatus.REJECTED_LIMIT != application.getStatus() && LoanStatus.REJECTED_EXCLUSION != application.getStatus()
                    && LoanStatus.REJECTED_KYC_FAILED != application.getStatus()) {
                // If status is somehow blocking again after lock acquisition (unlikely but possible race condition without DB transaction)
                throw new LoanApplicationException(
                        "State conflict: Application status " + application.getStatus() + " prevents new request.",
                        LoanStatus.FAILED_CONCURRENT
                );
            }

            // 4. Update application state for scoring
            application.setRequestedAmount(request.getAmount());
            application.setStatus(LoanStatus.PENDING_SCORE); // Mark ready for scoring
            application.setScoringToken(null); // Clear previous token if any
            application.setScore(null);
            application.setLimitAmount(null);
            application.setExclusionReason(null);
            application.setFailureMessage(null);
            application.setScoreQueryRetries(0); // Reset retries
            loanRepo.save(application); // Save state before async call

            // 5. Trigger Async Scoring Process
            scoringService.initiateAndQueryScoreAsync(customerNumber);
            log.info("Loan request for customer {} queued for scoring.", customerNumber);

            return application; // Return the application in PENDING_SCORE state

        } catch (Exception e) {
            // If anything fails synchronously after acquiring the lock, release it
            loanRepo.clearActiveProcess(customerNumber);
            // Re-throw or handle appropriately
            if (e instanceof LoanApplicationException) throw e;
            log.error("Unexpected error during loan request for {}: {}", customerNumber, e.getMessage(), e);
            throw new LoanApplicationException("Unexpected error processing loan request.");
        }
    }

    // --- Get Loan Status ---
    public LoanApplication getLoanStatus(String customerNumber) {
        log.debug("Fetching loan status for customer: {}", customerNumber);
        return loanRepo.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No loan application found for customer: " + customerNumber));
    }
}
