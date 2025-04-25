package io.credable.lms.repo;

import io.credable.lms.model.LoanApplication;

import java.util.Optional;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:29 pm
 */
public interface LoanApplicationRepository {
    Optional<LoanApplication> findByCustomerNumber(String customerNumber);
    void save(LoanApplication application);
    boolean hasActiveOrPendingLoan(String customerNumber);
    boolean lockCustomerForProcessing(String customerNumber);
    void clearActiveProcess(String customerNumber);
}
