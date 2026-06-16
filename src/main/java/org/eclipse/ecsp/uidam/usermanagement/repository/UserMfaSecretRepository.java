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

import org.eclipse.ecsp.uidam.usermanagement.entity.UserMfaSecretEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.MfaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserMfaSecretEntity}.
 */
public interface UserMfaSecretRepository extends JpaRepository<UserMfaSecretEntity, BigInteger> {

    /**
     * Find the most recent MFA record for a user regardless of status.
     *
     * @param username the user's username
     * @return optional entity
     */
    Optional<UserMfaSecretEntity> findTopByUsernameOrderByCreatedDateDesc(String username);

    /**
     * Find an MFA record by username and specific status.
     *
     * @param username the user's username
     * @param status   the desired MFA status
     * @return optional entity
     */
    Optional<UserMfaSecretEntity> findTopByUsernameAndStatusOrderByCreatedDateDesc(String username, MfaStatus status);

    /**
     * Find the most recent MFA record for a user by their canonical {@code userId},
     * regardless of status. Used by admin endpoints which are keyed by {@code userId}.
     *
     * @param userId the user's canonical id
     * @return optional entity
     */
    Optional<UserMfaSecretEntity> findTopByUserIdOrderByCreatedDateDesc(BigInteger userId);

    /**
     * Revoke all non-revoked records for a user (used before creating a new PENDING record on re-enroll).
     *
     * @param username the user's username
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserMfaSecretEntity m SET m.status = 'REVOKED' "
            + "WHERE m.username = :username AND m.status <> 'REVOKED'")
    void revokeAllActiveForUser(@Param("username") String username);

    /**
     * Revoke all non-revoked records for a user identified by {@code userId}.
     * Used by admin endpoints which are keyed by {@code userId}.
     *
     * @param userId the user's canonical id
     */
    @Transactional
    @Modifying
    @Query("UPDATE UserMfaSecretEntity m SET m.status = 'REVOKED' "
            + "WHERE m.userId = :userId AND m.status <> 'REVOKED'")
    void revokeAllActiveForUserId(@Param("userId") BigInteger userId);
}
