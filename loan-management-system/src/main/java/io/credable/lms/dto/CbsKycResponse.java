package io.credable.lms.dto;

import lombok.Data;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 4:31 pm
 */
@Data
public class CbsKycResponse {
    private String customerId;
    private String fullName;
    private String status;
    private String address;
    private String phoneNumber;
    private java.time.LocalDate dateOfBirth;
}
