package io.credable.lms.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:28 pm
 */
@Data
@NoArgsConstructor
public class LoanApplication {
    private String id = UUID.randomUUID().toString(); // Internal ID
    private String customerNumber;
    private BigDecimal requestedAmount;
    private LoanStatus status;
    private String scoringToken; // Token from initiateQueryScore
    private Integer score;
    private BigDecimal limitAmount;
    private String exclusionReason;
    private String failureMessage; // General failure reason
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private int scoreQueryRetries = 0;

    public LoanApplication(String customerNumber) {
        this.customerNumber = customerNumber;
        this.status = LoanStatus.PENDING_SUBSCRIPTION;
    }

    public LoanApplication(String customerNumber, BigDecimal requestedAmount) {
        this.customerNumber = customerNumber;
        this.requestedAmount = requestedAmount;
        this.status = LoanStatus.PENDING_SCORE;
    }
}
