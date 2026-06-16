package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

/**
 * Result of verifying a single MFA backup code.
 *
 * @param valid                {@code true} if the supplied code matched an unused backup code
 * @param remainingBackupCodes the number of unused backup codes left after this verification
 * @param lowBackupCodesWarning {@code true} when {@code remainingBackupCodes} is at or below the
 *                              low-codes threshold (so the UI can prompt the user to regenerate)
 */
public record MfaBackupCodeVerifyResponse(
        boolean valid,
        int remainingBackupCodes,
        boolean lowBackupCodesWarning
) {
}
