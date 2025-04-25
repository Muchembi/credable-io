package io.credable.lms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author - <a href="https://github.com/muchembi"> muchembi </a>
 * {@code @date} Thu, 24-Apr-2025, 7:20 pm
 */
@Data
public class ClientRegistrationRequest {
    // Fields match the POST request payload for /client/createClient
    private String url;
    private String name;
    private String username;
    private String password;
}
