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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JPA entity representing a single MFA backup (recovery) code for a user.
 *
 * <p>Backup codes are single-use. The plain code is shown to the user exactly once at
 * generation time; only a BCrypt hash is persisted here. A code is consumed by setting
 * {@code used = true} and recording {@code usedDate}.
 */
@Entity
@Table(name = "user_mfa_backup_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserMfaBackupCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "user_id", nullable = false)
    private BigInteger userId;

    @Column(name = "username", nullable = false)
    private String username;

    /** BCrypt hash of the plain backup code. */
    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_date")
    private Timestamp usedDate;

    @Column(name = "created_date", updatable = false)
    private Timestamp createdDate;

    @Column(name = "created_by")
    private String createdBy = "system";

    @PrePersist
    void onCreate() {
        this.createdDate = Timestamp.from(Instant.now());
    }
}
