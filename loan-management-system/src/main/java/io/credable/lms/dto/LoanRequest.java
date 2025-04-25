package io.credable.lms.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:40 pm
 */
@Data
public class LoanRequest {
    private String customerNumber;
    private BigDecimal amount;
}
