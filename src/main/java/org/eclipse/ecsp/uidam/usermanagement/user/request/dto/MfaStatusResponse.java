package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

/**
 * Response returned when querying a user's MFA enrollment status.
 *
 * @param enrolled {@code true} if the user has an ACTIVE MFA enrollment.
 * @param status   String representation of {@link org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus}
 *                 or {@code "NONE"} when no record exists.
 * @param backupCodesEnabled {@code true} when the tenant has MFA backup (recovery) codes enabled.
 *                 This is the single authoritative flag (tenant property
 *                 {@code mfa-backup-codes-enabled}) consumed by the authorization server to decide
 *                 whether backup-code pages and flows are shown.
 */
public record MfaStatusResponse(
        boolean enrolled,
        String status,
        boolean backupCodesEnabled
) {
}
