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
 *
 */

package org.eclipse.ecsp.uidam.usermanagement.repository;

import org.eclipse.ecsp.uidam.usermanagement.entity.UserRecoverySecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/**
 * user recovery secret repository.
 */
public interface UserRecoverySecretRepository extends JpaRepository<UserRecoverySecret, BigInteger> {
    @Query("select a from UserRecoverySecret a where a.recoverySecret=:recoverySecret"
            + " ORDER BY a.secretGeneratedAt DESC")
    UserRecoverySecret findUserRecoverySecretDetailsByRecoverySecret(
            @Param(value = "recoverySecret") String recoverySecret);

    @Transactional
    @Modifying
    @Query("UPDATE UserRecoverySecret a SET a.recoverySecretStatus = 'INVALIDATED'"
            + " WHERE a.userId = :userId AND a.recoverySecretStatus = 'GENERATED'")
    void invalidateOldRecoverySecret(@Param("userId") BigInteger userId);

}
