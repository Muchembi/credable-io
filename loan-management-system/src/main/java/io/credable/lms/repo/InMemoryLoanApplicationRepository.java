package io.credable.lms.repo;

import io.credable.lms.model.LoanApplication;
import io.credable.lms.model.LoanStatus;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:30 pm
 */
@Repository
public class InMemoryLoanApplicationRepository implements LoanApplicationRepository {

    private final Map<String, LoanApplication> store = new ConcurrentHashMap<>();
    private final Set<String> customersWithActiveProcess = ConcurrentHashMap.newKeySet();

    private static final Set<LoanStatus> BLOCKING_STATUSES = Set.of(
            LoanStatus.PENDING_SCORE,
            LoanStatus.SCORING_IN_PROGRESS,
            LoanStatus.ACTIVE
    );

    @Override
    public Optional<LoanApplication> findByCustomerNumber(String customerNumber) {
        return Optional.ofNullable(store.get(customerNumber));
    }

    @Override
    public void save(LoanApplication application) {
        application.setUpdatedAt(java.time.LocalDateTime.now());
        store.put(application.getCustomerNumber(), application);

        // Update active process tracking
        if (BLOCKING_STATUSES.contains(application.getStatus())) {
            customersWithActiveProcess.add(application.getCustomerNumber());
        } else {
            // Remove if the application moved to a non-blocking state
            customersWithActiveProcess.remove(application.getCustomerNumber());
        }
    }

    @Override
    public boolean hasActiveOrPendingLoan(String customerNumber) {
        // Check based on explicit tracking OR stored status
        return customersWithActiveProcess.contains(customerNumber) ||
                findByCustomerNumber(customerNumber)
                        .map(app -> BLOCKING_STATUSES.contains(app.getStatus()))
                        .orElse(false);
    }

    @Override
    public void clearActiveProcess(String customerNumber) {
        customersWithActiveProcess.remove(customerNumber);
        // Optionally update status if needed, e.g. if SCORING_FAILED should allow retry
        findByCustomerNumber(customerNumber).ifPresent(app -> {
            if (app.getStatus() == LoanStatus.SCORING_FAILED) {
                // Reset state or leave as failed - depends on desired retry behavior
                // For now, just to ensure the lock is released.
            }
        });
    }

    @Override
    public boolean lockCustomerForProcessing(String customerNumber) {
        return customersWithActiveProcess.add(customerNumber);
    }
}
