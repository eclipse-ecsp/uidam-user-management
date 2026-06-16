package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the MFA backup-code verification endpoint.
 *
 * <p>The backup code must be supplied in the request body rather than as a query
 * parameter so that it is not captured in access logs, tracing systems, or other
 * URL-level monitoring tooling.
 *
 * @param backupCode the plain-text single-use backup code entered by the user
 */
public record MfaBackupCodeVerifyRequest(
        @NotBlank String backupCode
) {
}
