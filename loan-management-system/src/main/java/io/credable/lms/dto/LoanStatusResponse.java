package io.credable.lms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:39 pm
 */
@Data
public class LoanStatusResponse {
    private String customerNumber;
    private String applicationId;
    private String status;
    private String message;
    private BigDecimal requestedAmount;
    private BigDecimal limitAmount;
    private Integer score;
}
