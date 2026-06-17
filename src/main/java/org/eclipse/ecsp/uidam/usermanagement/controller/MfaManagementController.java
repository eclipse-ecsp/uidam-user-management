package org.eclipse.ecsp.uidam.usermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.MfaManagementService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaEnrollInitiateResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaRecoveryKeyRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.Optional;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.STRING;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;

/**
 * REST controller exposing MFA (TOTP) enrollment management endpoints.
 *
 * <h2>Two groups of endpoints:</h2>
 * <ol>
 *   <li><strong>Internal auth-server endpoints</strong> ({@code /v1/users/{username}/mfa/**}) –
 *       called only from the authorization server (server-to-server, no API-gateway JWT
 *       requirement). These are not annotated with {@code @SecurityRequirement}.</li>
 *   <li><strong>Admin endpoints</strong> ({@code /v1/admin/users/{userId}/mfa/**}) –
 *       accessible via API-gateway with a {@code ManageUsers} scope. These are keyed by the
 *       canonical {@code userId} because the admin caller already holds the target user's id.</li>
 * </ol>
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class MfaManagementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MfaManagementController.class);

    private static final String MFA_BASE_PATH   = VERSION_V1 + "/users/{username}/mfa";
    private static final String ADMIN_MFA_PATH  = VERSION_V1 + "/admin/users/{userId}/mfa";
    private static final String USERNAME_VAR    = "username";
    private static final String USER_ID_VAR     = "userId";
    private static final String ADMIN_MFA_TAG   = "MFA Administration";

    private final MfaManagementService mfaManagementService;

    public MfaManagementController(MfaManagementService mfaManagementService) {
        this.mfaManagementService = mfaManagementService;
    }

    // ══════════════════════════════════════════════════════════
    //  Internal auth-server endpoints (no gateway JWT required)
    // ══════════════════════════════════════════════════════════

    /**
     * Initiate MFA enrollment: generate a new TOTP secret, persist as PENDING,
     * and return the secret + QR URI to the authorization server.
     *
     * @param username the user's username (path variable)
     * @return 200 with {@link MfaEnrollInitiateResponse}
     * @throws ResourceNotFoundException if the user does not exist
     */
    @PostMapping(MFA_BASE_PATH + "/enroll/initiate")
    public ResponseEntity<MfaEnrollInitiateResponse> initiateEnrollment(
            @PathVariable(USERNAME_VAR) String username) throws ResourceNotFoundException {
        LOGGER.info("[MFA] Initiate enrollment for user='{}'", username);
        MfaEnrollInitiateResponse response = mfaManagementService.initiateEnrollment(username);
        return ResponseEntity.ok(response);
    }

    /**
     * Activate MFA enrollment: mark the PENDING record as ACTIVE.
     * Called after the authorization server has successfully verified the user's first TOTP code.
     *
     * @param username the user's username (path variable)
     * @return 204 No Content
     * @throws ResourceNotFoundException if no PENDING enrollment exists
     */
    @PostMapping(MFA_BASE_PATH + "/enroll/activate")
    public ResponseEntity<Void> activateEnrollment(
            @PathVariable(USERNAME_VAR) String username) throws ResourceNotFoundException {
        LOGGER.info("[MFA] Activate enrollment for user='{}'", username);
        mfaManagementService.activateEnrollment(username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get MFA enrollment status for a user.
     *
     * @param username the user's username (path variable)
     * @return 200 with {@link MfaStatusResponse}
     */
    @GetMapping(MFA_BASE_PATH + "/status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(
            @PathVariable(USERNAME_VAR) String username) {
        LOGGER.info("[MFA] Get status for user='{}'", username);
        return ResponseEntity.ok(mfaManagementService.getStatus(username));
    }

    /**
     * Retrieve the TOTP secret for a user (ACTIVE or PENDING only).
     * Used by the authorization server during challenge validation.
     *
     * @param username the user's username (path variable)
     * @return 200 with the Base32 secret string, or 404 if not enrolled
     */
    @GetMapping(value = MFA_BASE_PATH + "/secret", produces = org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getMfaSecret(
            @PathVariable(USERNAME_VAR) String username) {
        LOGGER.info("[MFA] Get secret for user='{}'", username);
        Optional<String> secret = mfaManagementService.getSecret(username);
        return secret.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Revoke MFA enrollment for a user (re-enrollment trigger).
     * Called from the authorization server when user clicks "Re-enroll".
     *
     * @param username the user's username (path variable)
     * @return 204 No Content
     */
    @DeleteMapping(MFA_BASE_PATH + "/revoke")
    public ResponseEntity<Void> revokeEnrollment(
            @PathVariable(USERNAME_VAR) String username) {
        LOGGER.info("[MFA] Revoke enrollment for user='{}'", username);
        mfaManagementService.revokeEnrollment(username);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════
    //  MFA Recovery (email-based one-time security key)
    // ══════════════════════════════════════════════════════════

    /**
     * Send a 6-character one-time security key to the user's registered email address.
     * The key is valid for 10 minutes and can be used to reset MFA enrollment.
     *
     * @param username the user's username (path variable)
     * @return 200 OK on success
     * @throws ResourceNotFoundException if the user or MFA enrollment is not found
     */
    @PostMapping(MFA_BASE_PATH + "/recovery/send-key")
    public ResponseEntity<Void> sendRecoveryKey(
            @PathVariable(USERNAME_VAR) String username) throws ResourceNotFoundException {
        LOGGER.info("[MFA] Send recovery key for user='{}'", username);
        mfaManagementService.sendRecoveryKey(username);
        return ResponseEntity.ok().build();
    }

    /**
     * Verify the 6-character recovery key. On success, the existing MFA enrollment is revoked
     * and the user can re-enroll with a new authenticator.
     *
     * @param username the user's username (path variable)
     * @param request  the request body containing the 6-character key from the email
     * @return 200 with {@code true} if the key is valid; 200 with {@code false} otherwise
     * @throws ResourceNotFoundException if the user or MFA enrollment is not found
     */
    @PostMapping(MFA_BASE_PATH + "/recovery/verify-key")
    public ResponseEntity<Boolean> verifyRecoveryKey(
            @PathVariable(USERNAME_VAR) String username,
            @RequestBody @Valid MfaRecoveryKeyRequest request) throws ResourceNotFoundException {
        LOGGER.info("[MFA] Verify recovery key for user='{}'", username);
        boolean result = mfaManagementService.verifyRecoveryKeyAndRevoke(username, request.recoveryKey());
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════
    //  MFA Backup (recovery) codes
    // ══════════════════════════════════════════════════════════

    /**
     * Generate (or regenerate) a set of single-use MFA backup codes for the user.
     * The plain-text codes are returned exactly once; only hashes are stored.
     *
     * @param username the user's username (path variable)
     * @return 200 with {@link MfaBackupCodesResponse} containing the new codes
     * @throws ResourceNotFoundException if the user does not exist
     */
    @PostMapping(MFA_BASE_PATH + "/backup-codes/generate")
    public ResponseEntity<MfaBackupCodesResponse> generateBackupCodes(
            @PathVariable(USERNAME_VAR) String username) throws ResourceNotFoundException {
        LOGGER.info("[MFA] Generate backup codes for user='{}'", username);
        return ResponseEntity.ok(mfaManagementService.generateBackupCodes(username));
    }

    /**
     * Verify a single backup code. On success, the code is consumed (single-use).
     *
     * @param username the user's username (path variable)
     * @param request  the request body containing the plain-text backup code entered by the user
     * @return 200 with {@link MfaBackupCodeVerifyResponse}
     */
    @PostMapping(MFA_BASE_PATH + "/backup-codes/verify")
    public ResponseEntity<MfaBackupCodeVerifyResponse> verifyBackupCode(
            @PathVariable(USERNAME_VAR) String username,
            @RequestBody @Valid MfaBackupCodeVerifyRequest request) {
        LOGGER.info("[MFA] Verify backup code for user='{}'", username);
        return ResponseEntity.ok(mfaManagementService.verifyBackupCode(username, request.backupCode()));
    }

    // ══════════════════════════════════════════════════════════
    //  Admin endpoints (ManageUsers scope, via API-gateway)
    // ══════════════════════════════════════════════════════════

    /**
     * Admin: get MFA enrollment status for a user.
     *
     * @param loggedInUserId the admin user's ID from the API-gateway header
     * @param userId         the target user's canonical id
     * @return 200 with {@link MfaStatusResponse}
     */
    @Operation(
        summary = "Get MFA status (admin)",
        description = "Returns the MFA enrollment status for the specified user.",
        tags = {ADMIN_MFA_TAG},
        responses = {
            @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MfaStatusResponse.class)))
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
            schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @GetMapping(ADMIN_MFA_PATH + "/status")
    public ResponseEntity<MfaStatusResponse> adminGetMfaStatus(
            @RequestHeader(value = LOGGED_IN_USER_ID) String loggedInUserId,
            @PathVariable(USER_ID_VAR) BigInteger userId) {
        LOGGER.info("[MFA-ADMIN] Get status for userId='{}' by admin='{}'", userId, loggedInUserId);
        return ResponseEntity.ok(mfaManagementService.getStatusByUserId(userId));
    }

    /**
     * Admin: revoke MFA enrollment for a user (force re-enrollment on next login).
     *
     * @param loggedInUserId the admin user's ID from the API-gateway header
     * @param userId         the target user's canonical id
     * @return 204 No Content
     */
    @Operation(
        summary = "Revoke MFA enrollment (admin)",
        description = "Revokes the MFA enrollment for the specified user, forcing re-enrollment on next login.",
        tags = {ADMIN_MFA_TAG},
        responses = {
            @ApiResponse(responseCode = "204", description = "Enrollment revoked successfully")
        }
    )
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageUsers"})
    @Parameter(name = LOGGED_IN_USER_ID, description = LOGGED_IN_USER_ID,
            schema = @Schema(type = STRING), in = ParameterIn.HEADER)
    @DeleteMapping(ADMIN_MFA_PATH + "/revoke")
    public ResponseEntity<Void> adminRevokeEnrollment(
            @RequestHeader(value = LOGGED_IN_USER_ID) String loggedInUserId,
            @PathVariable(USER_ID_VAR) BigInteger userId) {
        LOGGER.info("[MFA-ADMIN] Revoke enrollment for userId='{}' by admin='{}'", userId, loggedInUserId);
        mfaManagementService.revokeEnrollmentByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
