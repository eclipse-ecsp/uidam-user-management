package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.sql.multitenancy.TenantContext;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaBackupCodeEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaSecretEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaBackupCodeRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserMfaSecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaEnrollInitiateResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaStatusResponse;
import org.eclipse.ecsp.uidam.usermanagement.utilities.InputSanitizer;
import org.eclipse.ecsp.uidam.usermanagement.utilities.MfaSecretEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Service handling all MFA (TOTP) enrollment lifecycle operations for the user-management service.
 *
 * <p>This service is the authoritative owner of MFA state. The authorization server calls
 * this service via REST (through {@code MfaManagementController}) to initiate/activate/revoke
 * enrollments and to retrieve secrets for TOTP validation at challenge time.
 */
@Service
public class MfaManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MfaManagementService.class);

    private static final String DEFAULT_ISSUER = "UIDAM";
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_PERIOD_SECONDS = 30;
    private static final int SECRET_BYTES = 20; // 160 bits – standard for TOTP
    private static final int GROUP_SIZE = 4;
    private static final int BITS_PER_BYTE = 8;
    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BASE32_CHAR = 5;
    private static final int BASE32_CHAR_MASK = 0x1F;
    private static final int RECOVERY_KEY_LENGTH = 6;
    private static final int RECOVERY_KEY_EXPIRY_MINUTES = 10;
    private static final String MFA_RECOVERY_NOTIFICATION_ID = "UIDAM_MFA_RECOVERY";
    private static final String RECOVERY_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String USERNAME_FIELD = "username";

    // Backup-code generation parameters
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int DEFAULT_BACKUP_CODE_COUNT = 8;
    private static final int LOW_BACKUP_CODES_THRESHOLD = 2;

    // Base32 alphabet (RFC 4648)
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final UserMfaSecretRepository mfaSecretRepository;
    private final UserMfaBackupCodeRepository mfaBackupCodeRepository;
    private final UsersRepository usersRepository;
    private final EmailNotificationService emailNotificationService;
    private final TenantConfigurationService tenantConfigurationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder recoveryKeyEncoder = new BCryptPasswordEncoder();

    /**
     * Constructs the MFA management service.
     *
     * @param mfaSecretRepository       repository for MFA secret persistence
     * @param mfaBackupCodeRepository   repository for MFA backup-code persistence
     * @param usersRepository           repository for user lookup
     * @param emailNotificationService  service for sending email notifications
     * @param tenantConfigurationService service for tenant-specific configuration
     */
    public MfaManagementService(UserMfaSecretRepository mfaSecretRepository,
                                UserMfaBackupCodeRepository mfaBackupCodeRepository,
                                UsersRepository usersRepository,
                                EmailNotificationService emailNotificationService,
                                TenantConfigurationService tenantConfigurationService) {
        this.mfaSecretRepository = mfaSecretRepository;
        this.mfaBackupCodeRepository = mfaBackupCodeRepository;
        this.usersRepository = usersRepository;
        this.emailNotificationService = emailNotificationService;
        this.tenantConfigurationService = tenantConfigurationService;
    }

    /**
     * Initiate a new MFA enrollment for the given username.
     *
     * <p>Revokes any existing non-revoked records for the user and creates a new PENDING record.
     *
     * @param username the user's username
     * @return {@link MfaEnrollInitiateResponse} containing the secret, OTP-auth URI and formatted manual key
     * @throws ResourceNotFoundException if the username does not exist
     */
    @Transactional
    public MfaEnrollInitiateResponse initiateEnrollment(String username) throws ResourceNotFoundException {
        username = normalizeUsername(username);
        List<UserEntity> users = usersRepository.findByUserName(username);
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("user", "userName", username);
        }
        UserEntity user = users.get(0);

        // Generate a new TOTP secret (Base32-encoded, 20 bytes / 160 bits)
        String secret = generateBase32Secret();

        // The table enforces a unique constraint on username (uq_mfa_username), so there can be
        // at most one row per user. Re-use the existing row (if any) instead of inserting a new
        // one to avoid a duplicate-key violation when re-enrolling after a revoke/recovery.
        UserMfaSecretEntity entity = mfaSecretRepository
                .findTopByUsernameOrderByCreatedDateDesc(username)
                .orElseGet(UserMfaSecretEntity::new);

        entity.setUserId(user.getId());
        entity.setUsername(username);
        entity.setTotpSecret(encryptSecret(secret));
        entity.setStatus(MfaStatus.PENDING);
        entity.setCreatedBy("system");
        // Clear any stale recovery key state from a previous enrollment cycle.
        entity.setRecoveryKey(null);
        entity.setRecoveryKeyExpiry(null);
        mfaSecretRepository.save(entity);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Enrollment initiated for user='{}'", InputSanitizer.forLog(username));
        }

        String qrUri = buildOtpAuthUri(username, secret);
        String manualKey = formatManualKey(secret);
        return new MfaEnrollInitiateResponse(secret, qrUri, manualKey);
    }

    /**
     * Activate the MFA enrollment for the given username by setting status to ACTIVE.
     *
     * <p>Called after the authorization server has verified the user's first TOTP code.
     *
     * @param username the user's username
     * @throws ResourceNotFoundException if no PENDING enrollment is found
     */
    @Transactional
    public void activateEnrollment(String username) throws ResourceNotFoundException {
        final String normalizedUsername = normalizeUsername(username);
        UserMfaSecretEntity entity = mfaSecretRepository
                .findTopByUsernameAndStatusOrderByCreatedDateDesc(normalizedUsername, MfaStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("mfa", USERNAME_FIELD, normalizedUsername));
        entity.setStatus(MfaStatus.ACTIVE);
        mfaSecretRepository.save(entity);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Enrollment activated for user='{}'", InputSanitizer.forLog(normalizedUsername));
        }
    }

    /**
     * Return the MFA enrollment status for the given username.
     *
     * @param username the user's username
     * @return {@link MfaStatusResponse} with enrolled flag, status string and the
     *         tenant's backup-codes-enabled flag
     */
    @Transactional(readOnly = true)
    public MfaStatusResponse getStatus(String username) {
        username = normalizeUsername(username);
        boolean backupCodesEnabled = isBackupCodesEnabled();
        Optional<UserMfaSecretEntity> opt =
                mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username);
        if (opt.isEmpty()) {
            return new MfaStatusResponse(false, "NONE", backupCodesEnabled);
        }
        UserMfaSecretEntity entity = opt.get();
        boolean enrolled = MfaStatus.ACTIVE == entity.getStatus();
        return new MfaStatusResponse(enrolled, entity.getStatus().name(), backupCodesEnabled);
    }

    /**
     * Return the TOTP secret for the given username (only if ACTIVE or PENDING).
     *
     * <p>The secret is returned <em>encrypted</em> (AES-256-GCM).  The authorization server is
     * responsible for decrypting it using the same per-tenant key before TOTP validation.
     *
     * @param username the user's username
     * @return Optional containing the encrypted Base64 secret blob, or empty if no active/pending enrollment
     */
    @Transactional(readOnly = true)
    public Optional<String> getSecret(String username) {
        username = normalizeUsername(username);
        return mfaSecretRepository.findTopByUsernameOrderByCreatedDateDesc(username)
                .filter(e -> e.getStatus() != MfaStatus.REVOKED)
                .map(UserMfaSecretEntity::getTotpSecret);
    }

    /**
     * Return the MFA enrollment status for the given {@code userId}.
     *
     * <p>Used by the admin endpoints, which are keyed by the canonical {@code userId}
     * (the admin caller already holds the target user's id, so no username lookup is needed).
     *
     * @param userId the user's canonical id
     * @return {@link MfaStatusResponse} with enrolled flag, status string and the
     *         tenant's backup-codes-enabled flag
     */
    @Transactional(readOnly = true)
    public MfaStatusResponse getStatusByUserId(BigInteger userId) {
        boolean backupCodesEnabled = isBackupCodesEnabled();
        Optional<UserMfaSecretEntity> opt =
                mfaSecretRepository.findTopByUserIdOrderByCreatedDateDesc(userId);
        if (opt.isEmpty()) {
            return new MfaStatusResponse(false, "NONE", backupCodesEnabled);
        }
        UserMfaSecretEntity entity = opt.get();
        boolean enrolled = MfaStatus.ACTIVE == entity.getStatus();
        return new MfaStatusResponse(enrolled, entity.getStatus().name(), backupCodesEnabled);
    }

    /**
     * Revoke the MFA enrollment for the given {@code userId}.
     *
     * <p>Used by the admin endpoints, which are keyed by the canonical {@code userId}.
     *
     * @param userId the user's canonical id
     */
    @Transactional
    public void revokeEnrollmentByUserId(BigInteger userId) {
        mfaSecretRepository.revokeAllActiveForUserId(userId);
        LOGGER.info("[MFA] Enrollment revoked for userId='{}'", userId);
    }

    /**
     * Revoke the MFA enrollment for the given username.
     *
     * <p>Called when a user or admin triggers re-enrollment.
     *
     * @param username the user's username
     */
    @Transactional
    public void revokeEnrollment(String username) {
        username = normalizeUsername(username);
        mfaSecretRepository.revokeAllActiveForUser(username);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Enrollment revoked for user='{}'", InputSanitizer.forLog(username));
        }
    }

    /**
     * Generate a 6-character recovery key, hash and store it with a 10-minute expiry,
     * and send it to the user's registered email address.
     *
     * @param username the user's username
     * @throws ResourceNotFoundException if the user or active MFA enrollment is not found
     */
    @Transactional
    public void sendRecoveryKey(String username) throws ResourceNotFoundException {
        final String normalizedUsername = normalizeUsername(username);
        List<UserEntity> users = usersRepository.findByUserName(normalizedUsername);
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("user", "userName", normalizedUsername);
        }
        final UserEntity user = users.get(0);

        UserMfaSecretEntity entity = mfaSecretRepository
                .findTopByUsernameAndStatusOrderByCreatedDateDesc(normalizedUsername, MfaStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("mfa enrollment", USERNAME_FIELD, normalizedUsername));

        // Generate plain-text 6-character key
        String plainKey = generateRecoveryKey();

        // Store BCrypt hash + expiry
        entity.setRecoveryKey(recoveryKeyEncoder.encode(plainKey));
        entity.setRecoveryKeyExpiry(
                Timestamp.from(Instant.now().plus(RECOVERY_KEY_EXPIRY_MINUTES, ChronoUnit.MINUTES)));
        mfaSecretRepository.save(entity);

        // Send email via existing notification infrastructure
        String email = user.getEmail();
        String name = buildDisplayName(user);

        Map<String, String> userDetails = new HashMap<>();
        userDetails.put(ApiConstants.EMAIL_ADDRESS, email);
        userDetails.put(ApiConstants.EMAIL_TO_NAME, name);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("name", name);
        notificationData.put("recoveryCode", plainKey);

        emailNotificationService.sendNotification(userDetails, MFA_RECOVERY_NOTIFICATION_ID, notificationData);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Recovery key sent to email for user='{}'", InputSanitizer.forLog(normalizedUsername));
        }
    }

    /**
     * Verify the 6-character recovery key. On success, revoke the existing enrollment
     * so the user can re-enroll with a new authenticator.
     *
     * @param username    the user's username
     * @param recoveryKey the plain-text 6-character key entered by the user
     * @return {@code true} if the key is valid and not expired; {@code false} otherwise
     * @throws ResourceNotFoundException if no active enrollment is found
     */
    @Transactional
    public boolean verifyRecoveryKeyAndRevoke(String username, String recoveryKey)
            throws ResourceNotFoundException {
        final String normalizedUsername = normalizeUsername(username);
        // Normalize recovery key defensively: trim whitespace, handle null, convert to uppercase
        final String candidate = recoveryKey == null ? "" : recoveryKey.trim().toUpperCase(Locale.ROOT);
        
        UserMfaSecretEntity entity = mfaSecretRepository
                .findTopByUsernameAndStatusOrderByCreatedDateDesc(normalizedUsername, MfaStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("mfa enrollment", USERNAME_FIELD, normalizedUsername));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Verifying recovery key for user='{}'", InputSanitizer.forLog(normalizedUsername));
        }
        if (entity.getRecoveryKey() == null || entity.getRecoveryKeyExpiry() == null) {
            LOGGER.warn("[MFA] Recovery key verification failed");
            return false;
        }

        if (Instant.now().isAfter(entity.getRecoveryKeyExpiry().toInstant())) {
            LOGGER.warn("[MFA] Recovery key expired");
            return false;
        }

        if (!recoveryKeyEncoder.matches(candidate, entity.getRecoveryKey())) {
            LOGGER.warn("[MFA] Recovery key mismatch");
            return false;
        }

        // Valid key – revoke enrollment
        mfaSecretRepository.revokeAllActiveForUser(normalizedUsername);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Recovery key verified – enrollment revoked for user='{}'",
                    InputSanitizer.forLog(normalizedUsername));
        }
        return true;
    }

    // ── backup codes ────────────────────────────────────────────────────────

    /**
     * Whether backup (recovery) codes are enabled for the current tenant.
     *
     * @return {@code true} if backup codes are enabled
     */
    public boolean isBackupCodesEnabled() {
        try {
            UserManagementTenantProperties props = tenantConfigurationService.getTenantProperties();
            if (props != null && props.getMfaBackupCodesEnabled() != null) {
                return props.getMfaBackupCodesEnabled();
            }
        } catch (Exception ex) {
            LOGGER.debug("[MFA] Could not resolve backup-codes-enabled flag, defaulting to true: {}",
                    ex.getMessage());
        }
        return true;
    }

    /**
     * Resolve the configured number of backup codes per set for the current tenant.
     *
     * @return the number of backup codes to generate
     */
    private int resolveBackupCodeCount() {
        try {
            UserManagementTenantProperties props = tenantConfigurationService.getTenantProperties();
            if (props != null && props.getMfaBackupCodesCount() != null && props.getMfaBackupCodesCount() > 0) {
                return props.getMfaBackupCodesCount();
            }
        } catch (Exception ex) {
            LOGGER.debug("[MFA] Could not resolve backup-codes-count, defaulting to {}: {}",
                    DEFAULT_BACKUP_CODE_COUNT, ex.getMessage());
        }
        return DEFAULT_BACKUP_CODE_COUNT;
    }

    /**
     * Generate (or regenerate) a fresh set of single-use backup codes for the user.
     *
     * <p>Any previously generated codes for the user are deleted. Only BCrypt hashes are
     * persisted; the plain-text codes are returned exactly once for one-time display.
     *
     * @param username the user's username
     * @return {@link MfaBackupCodesResponse} containing the plain-text codes (one-time)
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public MfaBackupCodesResponse generateBackupCodes(String username) throws ResourceNotFoundException {
        final String normalizedUsername = normalizeUsername(username);
        List<UserEntity> users = usersRepository.findByUserName(normalizedUsername);
        if (users == null || users.isEmpty()) {
            throw new ResourceNotFoundException("user", "userName", normalizedUsername);
        }
        final UserEntity user = users.get(0);

        // Clear any previously generated codes – a new set fully replaces the old one.
        mfaBackupCodeRepository.deleteAllForUser(normalizedUsername);

        int count = resolveBackupCodeCount();
        List<String> plainCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String plainCode = generateBackupCode();
            plainCodes.add(plainCode);

            UserMfaBackupCodeEntity entity = new UserMfaBackupCodeEntity();
            entity.setUserId(user.getId());
            entity.setUsername(normalizedUsername);
            entity.setCodeHash(recoveryKeyEncoder.encode(plainCode));
            entity.setUsed(false);
            entity.setCreatedBy("system");
            mfaBackupCodeRepository.save(entity);
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Generated {} backup codes for user='{}'",
                    count, InputSanitizer.forLog(normalizedUsername));
        }
        return new MfaBackupCodesResponse(plainCodes, count);
    }

    /**
     * Verify a single backup code. On a successful match, the code is consumed (marked used) atomically.
     *
     * <p>Matching is case-insensitive: codes are normalised to upper-case before hashing and
     * comparison. To prevent race conditions with concurrent verification attempts, code consumption
     * is performed atomically at the database level: only the first transaction to successfully
     * UPDATE the code row will succeed (checked via affected row count).
     *
     * @param username   the user's username
     * @param backupCode the plain-text backup code entered by the user
     * @return {@link MfaBackupCodeVerifyResponse} with validity and remaining-code count
     */
    @Transactional
    public MfaBackupCodeVerifyResponse verifyBackupCode(String username, String backupCode) {
        final String normalizedUsername = normalizeUsername(username);
        final String candidate = backupCode == null ? "" : backupCode.trim().toUpperCase(java.util.Locale.ROOT);

        // Read all unused codes to find a hash match (validation phase).
        List<UserMfaBackupCodeEntity> unused =
                mfaBackupCodeRepository.findByUsernameAndUsedFalse(normalizedUsername);

        UserMfaBackupCodeEntity matched = null;
        for (UserMfaBackupCodeEntity entity : unused) {
            if (recoveryKeyEncoder.matches(candidate, entity.getCodeHash())) {
                matched = entity;
                break;
            }
        }

        if (matched == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[MFA] Backup-code verification failed for user='{}'",
                        InputSanitizer.forLog(normalizedUsername));
            }
            long remaining = mfaBackupCodeRepository.countByUsernameAndUsedFalse(normalizedUsername);
            return new MfaBackupCodeVerifyResponse(false, (int) remaining,
                    remaining <= LOW_BACKUP_CODES_THRESHOLD);
        }

        // Atomically mark the code as used. The UPDATE is conditional on used=false to ensure
        // only one concurrent transaction can successfully consume this code. If another transaction
        // already consumed it, affectedRows will be 0 and we fail (as we should).
        int affectedRows = mfaBackupCodeRepository.consumeBackupCodeAtomically(matched.getId());
        
        if (affectedRows == 0) {
            // Another transaction already consumed this code before us.
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[MFA] Backup-code already consumed by another transaction for user='{}'",
                        InputSanitizer.forLog(normalizedUsername));
            }
            long remaining = mfaBackupCodeRepository.countByUsernameAndUsedFalse(normalizedUsername);
            return new MfaBackupCodeVerifyResponse(false, (int) remaining,
                    remaining <= LOW_BACKUP_CODES_THRESHOLD);
        }

        long remaining = mfaBackupCodeRepository.countByUsernameAndUsedFalse(normalizedUsername);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[MFA] Backup-code verified for user='{}', remaining={}",
                    InputSanitizer.forLog(normalizedUsername), remaining);
        }
        return new MfaBackupCodeVerifyResponse(true, (int) remaining, remaining <= LOW_BACKUP_CODES_THRESHOLD);
    }

    /**
     * Return the number of unused backup codes remaining for the user.
     *
     * @param username the user's username
     * @return remaining unused backup-code count
     */
    @Transactional(readOnly = true)
    public int getRemainingBackupCodeCount(String username) {
        return (int) mfaBackupCodeRepository.countByUsernameAndUsedFalse(normalizeUsername(username));
    }

    /**
     * Generate a single random alphanumeric backup code.
     *
     * @return a new plain-text backup code
     */
    private String generateBackupCode() {
        StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            sb.append(RECOVERY_CODE_CHARS.charAt(secureRandom.nextInt(RECOVERY_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    // ── internal helpers ────────────────────────────────────────────────────

    /**
     * Normalize a username to its canonical (lowercase, trimmed) form.
     *
     * <p>UIDAM stores and queries usernames in lowercase. MFA records, recovery-key
     * lookups and user lookups must all use the same canonical form, otherwise a user
     * who authenticates with a differently-cased username (e.g. {@code John} vs
     * {@code john}) would not be matched and a spurious "user not found" error results.
     *
     * @param username the raw username (may contain mixed case or surrounding whitespace)
     * @return the normalized username, or the original value if {@code null}
     */
    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Generate a cryptographically random Base32-encoded TOTP secret (20 bytes = 160 bits).
     */
    private String generateBase32Secret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        // Encode bytes to Base32
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << BITS_PER_BYTE) | (b & BYTE_MASK);
            bitsLeft += BITS_PER_BYTE;
            while (bitsLeft >= BITS_PER_BASE32_CHAR) {
                bitsLeft -= BITS_PER_BASE32_CHAR;
                sb.append(BASE32_ALPHABET.charAt((buffer >> bitsLeft) & BASE32_CHAR_MASK));
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (BITS_PER_BASE32_CHAR - bitsLeft)) & BASE32_CHAR_MASK));
        }
        return sb.toString();
    }

    private String buildOtpAuthUri(String username, String secret) {
        try {
            String issuer = resolveIssuer();
            // URLEncoder uses application/x-www-form-urlencoded rules, which encode spaces as '+'.
            // For otpauth:// URIs, authenticator apps expect '%20', so convert '+' back to '%20'.
            String account = urlEncodePath(issuer + ":" + username);
            String issuerEnc = urlEncodePath(issuer);
            return "otpauth://totp/" + account
                    + "?secret=" + secret
                    + "&issuer=" + issuerEnc
                    + "&digits=" + TOTP_DIGITS
                    + "&period=" + TOTP_PERIOD_SECONDS;
        } catch (Exception ex) {
            LOGGER.warn("[MFA] OTP-auth URI encoding failed: {}", ex.getMessage());
            return "otpauth://totp/UIDAM:" + username + "?secret=" + secret;
        }
    }

    /**
     * URL-encode a value for use in an otpauth:// URI, encoding spaces as {@code %20}
     * (rather than {@code +} as produced by {@link URLEncoder}).
     *
     * @param value the value to encode
     * @return the encoded value safe for otpauth URIs
     */
    private String urlEncodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Resolve the MFA issuer/app name from tenant properties, falling back to the default.
     *
     * @return the issuer name for the current tenant
     */
    private String resolveIssuer() {
        String appName = DEFAULT_ISSUER;
        String tenantId = null;
        try {
            UserManagementTenantProperties props = tenantConfigurationService.getTenantProperties();
            if (props != null && props.getMfaAppName() != null && !props.getMfaAppName().isBlank()) {
                appName = props.getMfaAppName();
            }
            tenantId = TenantContext.getCurrentTenant();
        } catch (Exception ex) {
            LOGGER.debug("[MFA] Could not resolve tenant MFA app name, using default: {}", ex.getMessage());
        }
        if (tenantId != null && !tenantId.isBlank() && !"default".equalsIgnoreCase(tenantId)) {
            return appName + "-" + tenantId;
        }
        return appName;
    }

    private String formatManualKey(String secret) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % GROUP_SIZE == 0) {
                sb.append(' ');
            }
            sb.append(secret.charAt(i));
        }
        return sb.toString();
    }

    private String generateRecoveryKey() {
        StringBuilder sb = new StringBuilder(RECOVERY_KEY_LENGTH);
        for (int i = 0; i < RECOVERY_KEY_LENGTH; i++) {
            sb.append(RECOVERY_CODE_CHARS.charAt(secureRandom.nextInt(RECOVERY_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String buildDisplayName(UserEntity user) {
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            name.append(user.getFirstName());
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(user.getLastName());
        }
        return name.toString().trim();
    }

    // ── MFA secret encryption helpers ───────────────────────────────────────

    /**
     * Encrypt a plain-text TOTP secret using the current tenant's encryption key/salt.
     *
     * @param plainSecret the raw Base32 TOTP secret
     * @return AES-256-GCM encrypted, Base64-encoded blob
     */
    private String encryptSecret(String plainSecret) {
        try {
            return MfaSecretEncryptionUtil.encrypt(
                    plainSecret,
                    resolveEncryptionKey(),
                    resolveEncryptionSalt());
        } catch (Exception ex) {
            LOGGER.error("[MFA] Failed to encrypt TOTP secret – aborting enrollment", ex);
            throw new MfaSecretEncryptionUtil.MfaEncryptionException("Failed to encrypt MFA secret", ex);
        }
    }

    /**
     * Resolve the MFA secret encryption key from tenant properties, with a safe fallback.
     */
    private String resolveEncryptionKey() {
        UserManagementTenantProperties props = tenantConfigurationService.getTenantProperties();
        return props.getMfaSecretEncryptionKey();
    }

    /**
     * Resolve the MFA secret encryption salt from tenant properties, with a safe fallback.
     */
    private String resolveEncryptionSalt() {
        UserManagementTenantProperties props = tenantConfigurationService.getTenantProperties();
        return props.getMfaSecretEncryptionSalt();
    }
}