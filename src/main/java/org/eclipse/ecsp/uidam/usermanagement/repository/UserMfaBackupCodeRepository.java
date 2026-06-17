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

package org.eclipse.ecsp.uidam.usermanagement.repository;

import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaBackupCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

/**
 * Spring Data JPA repository for {@link UserMfaBackupCodeEntity}.
 */
public interface UserMfaBackupCodeRepository extends JpaRepository<UserMfaBackupCodeEntity, BigInteger> {

    /**
     * Find all backup codes for a user (used or unused).
     *
     * @param username the user's username
     * @return list of backup-code entities
     */
    List<UserMfaBackupCodeEntity> findByUsername(String username);

    /**
     * Find all unused backup codes for a user.
     *
     * @param username the user's username
     * @return list of unused backup-code entities
     */
    List<UserMfaBackupCodeEntity> findByUsernameAndUsedFalse(String username);

    /**
     * Count the number of unused (remaining) backup codes for a user.
     *
     * @param username the user's username
     * @return count of unused backup codes
     */
    long countByUsernameAndUsedFalse(String username);

    /**
     * Delete all backup codes for a user. Used before regenerating a new set.
     *
     * @param username the user's username
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM UserMfaBackupCodeEntity b WHERE b.username = :username")
    void deleteAllForUser(@Param("username") String username);

    /**
     * Atomically consume (mark as used) a backup code if it is currently unused.
     *
     * <p>This method performs a conditional UPDATE that only succeeds if the code is not
     * already used. Used to prevent race conditions where two concurrent verification attempts
     * could both validate and consume the same code. Only the first to execute this UPDATE
     * will succeed (affected rows = 1); the second will see affected rows = 0.
     *
     * <p>The usedDate is set by the database via current_timestamp, ensuring the timestamp
     * reflects the exact moment of atomic consumption and is database-portable.
     *
     * @param id the backup code entity ID
     * @return number of affected rows (1 if successfully consumed, 0 if already used)
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserMfaBackupCodeEntity b SET b.used = true, b.usedDate = current_timestamp "
            + "WHERE b.id = :id AND b.used = false")
    int consumeBackupCodeAtomically(@Param("id") BigInteger id);
}
