/*
 * Copyright (c) 2023 - 2024 Harman International
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.ecsp.uidam.usermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JPA entity representing a user's TOTP MFA enrollment record.
 *
 * <p>One row per user (unique on {@code username}).  Status transitions:
 * {@code PENDING → ACTIVE} on first TOTP verification, {@code ACTIVE → REVOKED} on reset.
 */
@Entity
@Table(name = "user_mfa_secret")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserMfaSecretEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "user_id", nullable = false)
    private BigInteger userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /**
     * Base32-encoded TOTP secret.  Stored encrypted via {@code TotpSecretEncryptor} converter
     * when encryption is configured; stored as plain Base32 otherwise (dev mode).
     */
    @Column(name = "totp_secret", nullable = false)
    private String totpSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MfaStatus status = MfaStatus.PENDING;

    @Column(name = "created_date", updatable = false)
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;

    @Column(name = "created_by")
    private String createdBy = "system";

    /** Hashed 6-character recovery key sent via email. */
    @Column(name = "recovery_key")
    private String recoveryKey;

    /** Expiry timestamp for the recovery key. */
    @Column(name = "recovery_key_expiry")
    private Timestamp recoveryKeyExpiry;

    @PrePersist
    void onCreate() {
        Timestamp now = Timestamp.from(Instant.now());
        this.createdDate = now;
        this.updatedDate = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedDate = Timestamp.from(Instant.now());
    }
}
