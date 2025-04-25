package io.credable.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:41 pm
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreQueryResponse {
    private Long id;
    private String customerNumber;
    private Integer score;
    private BigDecimal limitAmount;
    private String exclusion;
    private String exclusionReason;
}
