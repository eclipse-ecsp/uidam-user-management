package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

import java.util.List;

/**
 * Response returned when generating (or regenerating) a set of MFA backup codes.
 *
 * <p>The plain-text codes are returned exactly once at generation time; only their
 * BCrypt hashes are persisted. The caller must display these to the user and discard them.
 *
 * @param codes the freshly generated plain-text backup codes (one-time display)
 * @param count the number of codes generated
 */
public record MfaBackupCodesResponse(
        List<String> codes,
        int count
) {
}
