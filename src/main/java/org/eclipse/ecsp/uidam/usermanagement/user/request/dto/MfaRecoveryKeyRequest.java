package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the MFA recovery-key verification endpoint.
 *
 * <p>The recovery key must be supplied in the request body rather than as a query
 * parameter so that it is not captured in access logs, tracing systems, or other
 * URL-level monitoring tooling.
 *
 * @param recoveryKey the 6-character one-time recovery key sent to the user's email
 */
public record MfaRecoveryKeyRequest(
        @NotBlank String recoveryKey
) {
}
