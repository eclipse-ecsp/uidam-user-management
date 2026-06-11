package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

/**
 * Response returned when initiating a new MFA TOTP enrollment.
 *
 * @param secret    Base32-encoded TOTP secret (shown once to the user for manual entry).
 * @param qrUri     Full {@code otpauth://} URI suitable for QR-code generation.
 * @param manualKey Human-readable, space-separated groups of 4 characters of the secret.
 */
public record MfaEnrollInitiateResponse(
        String secret,
        String qrUri,
        String manualKey
) {
}
