package org.eclipse.ecsp.uidam.security.policy.handler;

import java.sql.Timestamp;

/**
 * Input class for password validation, containing the username, password, and last update time.
 */
public record PasswordValidationInput(String username, String password, Timestamp lastUpdateTime) {
}
