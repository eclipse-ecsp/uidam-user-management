package org.eclipse.ecsp.uidam.usermanagement.user.response.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the password policy response DTO.
 * This class contains the details of the password policy to be enforced while password creation.
 */
@Getter
@Setter
public class PasswordPolicyResponse {
    private int minLength;
    private int maxLength;
    private int minConsecutiveLettersLength;
    private String passwordRegex;

    // private boolean requireUppercase;
    // private boolean requireLowercase;
    // private boolean requireDigit;
    // private boolean requireSpecialCharacter;
}
