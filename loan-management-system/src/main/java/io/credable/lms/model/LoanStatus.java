package io.credable.lms.model;

/**
 *
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:26 pm
 */
public enum LoanStatus {
    PENDING_SUBSCRIPTION, // Initial state after subscribe call (optional)
    ELIGIBLE,             // Eligible after KYC check (optional intermediate)
    PENDING_SCORE,        // Loan requested, waiting for score query completion
    SCORING_IN_PROGRESS,  // Explicitly indicates scoring is running (optional refinement)
    SCORING_FAILED,       // Scoring failed after retries
    APPROVED,             // Score received, limit >= requested amount
    REJECTED_LIMIT,       // Score received, limit < requested amount
    REJECTED_EXCLUSION,   // Score received, customer excluded
    REJECTED_KYC_FAILED,  // KYC check failed during subscription/request
    ACTIVE,               // Loan approved and considered disbursed (simplified)
    FAILED_CONCURRENT     // Attempted request while another was active/pending
}
